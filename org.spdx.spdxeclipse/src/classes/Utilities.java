/**
 * Copyright © 2015 University of Nebraska at Omaha
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
*/

package classes;

import java.io.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.ui.*;

import classes.ExceptionUtilities;

public class Utilities {
		
	// Takes a target directory and a filename as parameters and runs the DoSPDX.py 
	// command on the combined target directory and file. The output of the command is
	// written to a .spdx file that is named the same as the tar file name.
	public Boolean CreateSPDX(String target_directory, String tar_file_name, String spdx_document_type)
	{ 
		try
		{
			String DoSPDXLoc = null;
			String dospdxOutput = null;
			String target_spdxfile = target_directory + "/" + tar_file_name + ".spdx";
			
			tar_file_name += ".tar";
			
			Process findDoSPDX = Runtime.getRuntime().exec("locate DoSPDX.py");
			
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(findDoSPDX.getInputStream()));
			
			if ((DoSPDXLoc = bufferedReader.readLine()) != null)
			{
				File spdxFile = new File(target_spdxfile);
				
				if(!spdxFile.exists())
				{
					spdxFile.createNewFile();
				}
				
				// Put all the pieces of the DoSPDX command together and run it
				String DoSPDXcmd = DoSPDXLoc + " -p " + target_directory + "/" + tar_file_name + " --scan --scanOption fossology --print " + spdx_document_type;

				Process spdxDocInput = Runtime.getRuntime().exec(DoSPDXcmd);
				
				// Create a buffered reader and writer for capturing the output of the process running the DoSPDX
				// command the for writing the captured output to the .spdx file.
				BufferedReader spdxOutput = new BufferedReader(new InputStreamReader(spdxDocInput.getInputStream()));
				BufferedWriter spdxFileWriter = new BufferedWriter(new FileWriter(spdxFile));
				
				while((dospdxOutput = spdxOutput.readLine()) != null)
				{
					spdxFileWriter.append(dospdxOutput + "\n");
				}
				
				// Remove the .tar file used by the DoSPDX command to clean up the directory. 
				// Then close the buffered reader and writer.
				String rmtarcmd = "rm -f " + target_directory + "/" + tar_file_name;
				@SuppressWarnings("unused")
				Process RemoveTarFile = Runtime.getRuntime().exec(rmtarcmd);
				
				spdxOutput.close();
				spdxFileWriter.close();
			}
			
			bufferedReader.close();
        }
		catch (Exception e)
		{			
			ExceptionUtilities exceptionUtils = new ExceptionUtilities();
			
			exceptionUtils.Error(e);
			
			return false;
		}
		
		return true;
	}
	
	public String CreateSPDXDirectory() 
	{
		IPath proj_dir = GetProjectDirectory();
		
		String tar_fp = proj_dir.append("/SPDX").toOSString();
		
		File file = new File(tar_fp);
				
		if (!file.exists())
		{			
			file.mkdirs();
		}
		
		return tar_fp;
	}
	
	// This method executes library functions for creating .tar
	// file of the file specified in the filepath parameter.
	public Boolean CreateTarball(String target_directory, String tar_file_name, String file_directory)
	{
		try 
		{
			Runtime.getRuntime().exec("tar -zcPf " + target_directory + "/" + tar_file_name + ".tar " + file_directory);
		} 
		catch (Exception e) 
		{
			ExceptionUtilities exceptionUtils = new ExceptionUtilities();
			
			exceptionUtils.Error("An error occured while creating the tarball for the currently open file.  Please try your request again.", e);
		
			return false;
		}
		
		return true;
	}
	
	// Returns the absolute directory path of the currently open file in the editor
	public String GetFileAbsolutePath() throws FileNotFoundException
	{		
		String filepath = null;
		
		IWorkbenchPart workbenchpart = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActivePart();
		IFile ifile = (IFile) workbenchpart.getSite().getPage().getActiveEditor().getEditorInput().getAdapter(IFile.class);
		
		if(ifile != null) 
		{
			filepath = ifile.getRawLocation().makeAbsolute().toOSString();
		}
		else
		{
			throw new FileNotFoundException();
		}
		
		return filepath;
	}
	
	/**
	 *  This method first gets the users current workspace and then grabs the file currently open.  Using the
	 *  IFile method .getName() we obtain the name of the currently open file.
	 * <p>
	 * @return Name of the currently open file in the form of a String.
	*/
	public String GetOpenFilename() throws FileNotFoundException
	{		
		String filename = null;

		// Grab the users current workspace.
		IWorkbenchPart workbenchpart = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActivePart();
		
		// Grab the currently open file if one exists.
		IFile ifile = (IFile) workbenchpart.getSite().getPage().getActiveEditor().getEditorInput().getAdapter(IFile.class);
		
		// If a file was open...
		if (ifile != null)
		{
			// Grab the name.
			filename = ifile.getName();
		}
		// If no file was open...
		else
		{
			throw new FileNotFoundException();
		}
		
		return filename;
	}
	
	/**
	 * Using the root workspace directory for a users eclipse instance and the directory for the 
	 * currently open file, this method parses each location and finds the project directory for the open file.
	 * <p>
	 * @return IPath of the Project Directory
	*/
	public IPath GetProjectDirectory()
	{
		// Get the Workspace Directory.
		IPath wksp_dir = GetWorkspaceDirectory();

		int forwardSlashes = 0;
		
		// Count the number of directories for Workspace Directory.
		for(int i = 0; i < wksp_dir.toOSString().length(); i++) 
		{
		    if(wksp_dir.toOSString().charAt(i) == '/')
		    {
		    	forwardSlashes++;
		    }
		}
			
		String filepath = null;
		
		try 
		{
			// Get the location of the currently open file.
			filepath = GetFileAbsolutePath();
		} 
		catch (Exception e) 
		{
			ExceptionUtilities exceptionUtils = new ExceptionUtilities();
			
			exceptionUtils.Error("An error occured while getting the absolute path of the currently open file.  Please try your request again.", e);
		}
		
		// Parse the directories.
		String[] directories = filepath.split("/");
		
		// Get the project name.
		String proj_name = directories[forwardSlashes+1];
		
		// Append the project directory to the workspace directory.
		IPath proj_dir = wksp_dir.append("/" + proj_name + "");
			
		return proj_dir;
	}
	
	/**
	 * This method uses the IWorkspace method ResourcesPlugin.getWorkspace() to obtain the root workspace
	 * directory for a users eclipse instance.
	 * <p>
	 * @return IPath of the Workspace Directory
	*/
	public IPath GetWorkspaceDirectory()
	{
		// Get the current workspace directory.
		IPath wksp_dir = ResourcesPlugin.getWorkspace().getRoot().getLocation();
				
		return wksp_dir;
	}
	
	/**
	 * This method refreshes a users workspace manually using the IWorkspaceRoot's refreshLocal() method.
	*/
	public void RefreshInstance()
	{
		// Get the current workspace directory.
		IWorkspaceRoot workspaceroot = ResourcesPlugin.getWorkspace().getRoot();
		
		try 
		{
			// Try to refresh the workspace.
			workspaceroot.refreshLocal(IResource.DEPTH_INFINITE, null);
		} 
		catch (Exception e) 
		{
			ExceptionUtilities exceptionUtils = new ExceptionUtilities();
			
			exceptionUtils.Error("An error occured while refreshing your workspace.  Please manually refresh your workspace using the right clickable menu.", e);
		}
	}
}