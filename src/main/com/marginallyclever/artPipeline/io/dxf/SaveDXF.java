package com.marginallyclever.artPipeline.io.dxf;

import java.awt.Component;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import javax.swing.filechooser.FileNameExtensionFilter;

import com.marginallyclever.artPipeline.ImageManipulator;
import com.marginallyclever.artPipeline.io.SaveResource;
import com.marginallyclever.convenience.MathHelper;
import com.marginallyclever.convenience.log.Log;
import com.marginallyclever.convenience.turtle.Turtle;
import com.marginallyclever.convenience.turtle.TurtleMove;
import com.marginallyclever.makelangelo.Translator;
import com.marginallyclever.makelangeloRobot.MakelangeloRobot;
import com.marginallyclever.makelangeloRobot.settings.MakelangeloRobotSettings;

/**
 * Reads in DXF file and converts it to a temporary gcode file, then calls LoadGCode. 
 * @author Dan Royer
 *
 */
public class SaveDXF extends ImageManipulator implements SaveResource {
	private static FileNameExtensionFilter filter = new FileNameExtensionFilter(Translator.get("FileTypeDXF"), "dxf");
	
	@Override
	public String getName() {
		return "DXF";
	}
	
	@Override
	public FileNameExtensionFilter getFileNameFilter() {
		return filter;
	}

	@Override
	public boolean canSave(String filename) {
		String ext = filename.substring(filename.lastIndexOf('.'));
		return (ext.equalsIgnoreCase(".dxf"));
	}

	@Override
	/**
	 * see http://paulbourke.net/dataformats/dxf/min3d.html for details
	 * @param outputStream where to write the data
	 * @param robot the robot from which the data is obtained
	 * @return true if save succeeded.
	 */
	public boolean save(OutputStream outputStream, MakelangeloRobot robot, Component parentComponent) {
		Log.message("saving...");
		Turtle turtle = robot.getTurtle();
		MakelangeloRobotSettings settings = robot.getSettings();
		
		try(OutputStreamWriter out = new OutputStreamWriter(outputStream)) {
			// header
			out.write("999\nDXF created by Makelangelo software (http://makelangelo.com)\n");
			out.write("0\nSECTION\n");
			out.write("2\nHEADER\n");
			out.write("9\n$ACADVER\n1\nAC1006\n");
			out.write("9\n$INSBASE\n");
			out.write("10\n"+settings.getPaperLeft()+"\n");
			out.write("20\n"+settings.getPaperBottom()+"\n");
			out.write("30\n0.0\n");
			out.write("9\n$EXTMIN\n");
			out.write("10\n"+settings.getPaperLeft()+"\n");
			out.write("20\n"+settings.getPaperBottom()+"\n");
			out.write("30\n0.0\n");
			out.write("9\n$EXTMAX\n");
			out.write("10\n"+settings.getPaperRight()+"\n");
			out.write("20\n"+settings.getPaperTop()+"\n");
			out.write("30\n0.0\n");
			out.write("0\nENDSEC\n");

			// tables section
			out.write("0\nSECTION\n");
			out.write("2\nTABLES\n");
			// line type
			out.write("0\nTABLE\n");
			out.write("2\nLTYPE\n");
			out.write("70\n1\n");
			out.write("0\nLTYPE\n");
			out.write("2\nCONTINUOUS\n");
			out.write("70\n64\n");
			out.write("3\nSolid line\n");
			out.write("72\n65\n");
			out.write("73\n0\n");
			out.write("40\n0.000\n");
			out.write("0\nENDTAB\n");
			// layers
			out.write("0\nTABLE\n");
			out.write("2\nLAYER\n");
			out.write("70\n6\n");
			out.write("0\nLAYER\n");
			out.write("2\n1\n");
			out.write("70\n64\n");
			out.write("62\n7\n");
			out.write("6\nCONTINUOUS\n");
			out.write("0\nLAYER\n");
			out.write("2\n2\n");
			out.write("70\n64\n");
			out.write("62\n7\n");
			out.write("6\nCONTINUOUS\n");
			out.write("0\nENDTAB\n");
			out.write("0\nTABLE\n");
			out.write("2\nSTYLE\n");
			out.write("70\n0\n");
			out.write("0\nENDTAB\n");
			// end tables
			out.write("0\nENDSEC\n");

			// empty blocks section (good form?)
			out.write("0\nSECTION\n");
			out.write("0\nBLOCKS\n");
			out.write("0\nENDSEC\n");
			// now the lines
			out.write("0\nSECTION\n");
			out.write("2\nENTITIES\n");

			boolean isUp=true;
			double x0 = settings.getHomeX();
			double y0 = settings.getHomeY();
			
			String matchUp = settings.getPenUpString();
			String matchDown = settings.getPenDownString();
			
			if(matchUp.contains(";")) {
				matchUp = matchUp.substring(0, matchUp.indexOf(";"));
			}
			matchUp = matchUp.replaceAll("\n", "");

			if(matchDown.contains(";")) {
				matchDown = matchDown.substring(0, matchDown.indexOf(";"));
			}
			matchDown = matchDown.replaceAll("\n", "");
			
			
			for( TurtleMove m : turtle.history ) {
				switch(m.type) {
				case TRAVEL:
					isUp=true;
					x0=m.x;
					y0=m.y;
					break;
				case DRAW:
					if(isUp) isUp=false;
					else {
						out.write("0\nLINE\n");
						out.write("8\n1\n");  // layer 1
						out.write("10\n"+MathHelper.roundOff3(x0)+"\n");
						out.write("20\n"+MathHelper.roundOff3(y0)+"\n");
						out.write("11\n"+MathHelper.roundOff3(m.x)+"\n");
						out.write("21\n"+MathHelper.roundOff3(m.y)+"\n");
					}
					x0=m.x;
					y0=m.y;
					
					break;
				case TOOL_CHANGE:
					// TODO write out DXF layer using  m.getColor()
					break;
				}
			}
			// wrap it up
			out.write("0\nENDSEC\n");
			out.write("0\nEOF\n");
			out.flush();
		}
		catch(IOException e) {
			Log.error(Translator.get("SaveError") +" "+ e.getLocalizedMessage());
			return false;
		}
		
		Log.message("done.");
		return true;
	}

}
