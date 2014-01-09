package fitnesse.Tfs;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import fitnesse.wiki.fs.*;
import fitnesse.wiki.VersionInfo;

public class TFSVersionsController implements VersionsController
{
	private static final Logger LOG = Logger.getLogger(TFSVersionsController.class.getName());
	private final String TFSPath;

	private final FileSystem fileSystem;

	public TFSVersionsController()
	{
		fileSystem 	= new DiskFileSystem();
		TFSPath 	= System.getenv( "TFS_Path" );
	}

	@Override
	public void setHistoryDepth(int historyDepth)
	{
		String lText = String.format( "setHistoryDepth %d", historyDepth );
		LOG.log( Level.INFO, lText );
	}

	@Override
	public FileVersion[] getRevisionData(String label, File... files)
	{
		String lText = String.format( "getRevisionData label=%s, ", label );

		FileVersion[] versions = new FileVersion[files.length];
		int counter = 0;
		for (File file : files)
		{
			if (fileSystem.exists(file))
			{
				lText += String.format( "\n -- fileName = %s", file.getPath() );
				versions[counter++] = new RevisionFileVersion(file, "");
			}
		}

		return versions;
	}

	private class RevisionFileVersion implements FileVersion
	{
		private final File file;
		private final String author;

		private RevisionFileVersion(File file, String author)
		{
			this.file = file;
			this.author = author;
		}

		@Override
		public File getFile()
		{
			return file;
		}

		@Override
		public InputStream getContent() throws IOException {
			return new BufferedInputStream(fileSystem.getInputStream(file));
		}

		@Override
		public String getAuthor()
		{
			return author;
		}

		@Override
		public Date getLastModificationTime()
		{
			return new Date( fileSystem.lastModified(file) );
		}
	}

	@Override
	public Collection<VersionInfo> history(File... files)
	{
		LOG.log( Level.INFO, "history" );
		return Collections.emptyList();
	}

	@Override
	public VersionInfo makeVersion(FileVersion... fileVersions) throws IOException
	{
		//String lText = String.format( "makeVersion files: \n %s", getFileNames( fileVersions ) );
		//LOG.log( Level.INFO, lText );

		for (FileVersion fileVersion : fileVersions)
		{
			File lFile = fileVersion.getFile();
		
			addDirectory( lFile.getParentFile() );
			
			boolean lIsNew = !fileSystem.exists( lFile );			

			// update file content
			InputStream content = fileVersion.getContent();
			try
			{
				fileSystem.makeFile( lFile, content );
			}
			finally
			{
				content.close();
			}
			runTF( lIsNew ? "add" : "checkout", lFile.getPath() );
		}
		
		return VersionInfo.makeVersionInfo(fileVersions[0].getAuthor(), fileVersions[0].getLastModificationTime());
	}

	@Override
	public void delete(FileVersion... fileVersions)
	{
		String lText = String.format( "delete files: \n %s", getFileNames( fileVersions ) );
		LOG.log( Level.INFO, lText );

		for (FileVersion fileVersion : fileVersions)
		{
			deleteFile( fileVersion.getFile() );			
		}
	}
	
	private void deleteFile(File file)
	{
		if (file.isDirectory())
		{
			for(File f : file.listFiles())
				deleteFile( f );
		}
		
		if( IsFileCheckedOut(file) )
		{
			runTF( "undo", file.getPath() );
			fileSystem.delete( file );
		}
		else
			runTF( "delete", file.getPath() );
	}
	
	private boolean IsFileCheckedOut(File file)
	{
		String	lResult = runTF( "status", file.getPath() );
		
		return lResult.split( "\n" ).length > 1;
	}

	private String getFileNames(FileVersion... fileVersions)
	{
		String	lText;
		String	lRet = "";

		for (FileVersion fileVersion : fileVersions)
		{
		  lText = String.format( "File: %s\n", fileVersion.getFile().getPath() );
		  lRet += lText;
		}

		return lRet;
	}

	@Override
	public VersionInfo addDirectory(final FileVersion dir) throws IOException
	{
		LOG.log( Level.INFO, "addDirectory" );

		final File filePath = dir.getFile();
		addDirectory(filePath);
		return VersionInfo.makeVersionInfo(dir.getAuthor(), new Date(fileSystem.lastModified(filePath)));
	}

	private void addDirectory(final File filePath) throws IOException
	{
		if (!fileSystem.exists(filePath))
		{
			fileSystem.makeDirectory(filePath);
			runTF( "add", filePath.getPath() );
		}
	}

	@Override
	public void rename(FileVersion fileVersion, File oldFile) throws IOException
	{
		LOG.log( Level.INFO, "rename" );

		fileSystem.rename(fileVersion.getFile(), oldFile);
	}
  
	public String runTF(String option, String file)
	{
		return execute( TFSPath + "\\tf.exe " + option + " " + file );
	}

	private String execute(String command)
	{
		String result = "";
		Process process;
		
		try
		{
			process = Runtime.getRuntime().exec( command );

			process.waitFor();
			result = getOutput( process );
		}
		catch (Exception e1)
		{
			// TODO Auto-generated catch block
			result = "Error: " + e1.getMessage();
		}

		return result;
	}

	private String getOutput(Process process) throws IOException 
	{
		String result = "";
		String line;
		BufferedReader input = new BufferedReader( new InputStreamReader(process.getInputStream()) );
		
		while ((line = input.readLine()) != null)
		{
			result += line + "\n";
		}
		input.close();
		
		return result;
	}
}
