/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package games.strategy.engine.framework.ui;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.io.File;

import games.strategy.engine.framework.GameRunner;

/**
 * <p>Title: TripleA</p>
 * <p> </p>
 * <p> Copyright (c) 2002</p>
 * <p> </p>
 * @author Sean Bridges
 *
 */

public class SaveGameFileChooser extends JFileChooser
{

	public static final File DEFAULT_DIRECTORY = new File(GameRunner.getRootFolder(), "/savedGames/");
	public static final String AUTOSAVE_FILE_NAME = "autosave.svg";

	private static SaveGameFileChooser s_instance;

	public static SaveGameFileChooser getInstance()
	{
		if(s_instance == null)
			s_instance = new SaveGameFileChooser();
		return s_instance;
	}

    public SaveGameFileChooser()
    {
	    super();
		setFileFilter(m_gameDataFileFilter);
		ensureDefaultDirExists();
		setCurrentDirectory(DEFAULT_DIRECTORY);
    }

	public static void ensureDefaultDirExists()
	{
		if(!DEFAULT_DIRECTORY.exists())
		{
			try
			{
				DEFAULT_DIRECTORY.mkdir();
			} catch(Exception e){e.printStackTrace();}
		}
	}


	FileFilter m_gameDataFileFilter = new FileFilter()
	{
		public  boolean accept(File f)
		{
			if (f.isDirectory())
				return true;

			return f.getName().endsWith(".svg");
		}

		public String getDescription()
		{
		    return "Saved Games, *.svg";
		}
	};
}

