package vialab.SMT;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import processing.core.PApplet;
import processing.core.PConstants;
import processing.opengl.PGL;
import processing.opengl.PGraphicsOpenGL;

import java.nio.ByteBuffer;

import javax.media.opengl.GL;

class SMTZonePicker {
	
	PGraphics3D picking_context;
	
	private final static int BG_PICK_COLOR = 0x00ffffff;
	private final int START_PICK_COLOR = 0;
	// PICK_COLOR_INC needs a a value that is fairly large to tell the
	// difference between few zones by eye, and need to have a lcm(I,N)=IxN
	// where I is PICK_COLOR_INC and N is the max number of pickColors
	// lcm(I,N)=IxN means that the the pick color will only loop after being
	// added N times, and so assures that we use all pickColors
	// 74 is a valid solution for I when N is 2^8-1, 2^16-1, or 2^24-1
	private final int PICK_COLOR_INC = 74;

	private int currentPickColor = START_PICK_COLOR;

	private Map<Integer, Zone> zonesByPickColor = Collections
			.synchronizedMap(new LinkedHashMap<Integer, Zone>());

	private SortedSet<Integer> activePickColors = new TreeSet<Integer>();

	int SIZEOF_INT = Integer.SIZE / 8;

	public SMTZonePicker() {
		this.picking_context = (PGraphics3D) SMT.applet.createGraphics(
			SMT.renderer.width, SMT.renderer.height, PConstants.P3D);

		// add the background color mapping to null
		zonesByPickColor.put(BG_PICK_COLOR, null);
	}

	public void add(Zone zone) {
		if (activePickColors.size() == BG_PICK_COLOR)
			// This means every color from 0 to BG_PICK_COLOR-1 has been used,
			// although this should not really occur in use
			System.err.printf(
				"Warning, added zone is unpickable, maximum is %d pickable zones\n",
				BG_PICK_COLOR);
		else {
			if( ! zonesByPickColor.containsValue( zone)) {
				zone.setPickColor( currentPickColor);
				zonesByPickColor.put( currentPickColor, zone);
				activePickColors.add( currentPickColor);

				do {
					currentPickColor += PICK_COLOR_INC;
					// mod by max/background color, so as to wrap around and
					// never reach it
					currentPickColor %= BG_PICK_COLOR;
				} while (activePickColors.contains(currentPickColor)
						&& activePickColors.size() < BG_PICK_COLOR);

				for (Zone child : zone.children)
					this.add(child);
			}
		}
	}

	public boolean contains(Zone zone) {
		return zonesByPickColor.containsValue(zone);
	}

	public Zone remove(Zone zone) {
		activePickColors.remove( zone.getPickColor());
		Zone removed = zonesByPickColor.remove( zone.getPickColor());
		zone.setPickColor( -1);
		return removed;
	}

	public Zone pick(Touch t) {
		int pickColor = -1;

		// prevent ArrayOutOfBoundsException, although maybe this should be done
		// in Touch itself
		int x = t.x;
		int y = t.y;
		if (t.y >= SMT.renderer.height)
			y = SMT.renderer.height - 1;
		if (t.x >= SMT.renderer.width)
			x = SMT.renderer.width - 1;
		if (t.y < 0)
			y = 0;
		if (t.x < 0)
			x = 0;

		PGL pgl = SMT.renderer.beginPGL();
		// force fallback until 2.0b10
		if( ! SMT.fastPicking || pgl == null)
			// really slow way(max 70 fps on a high end card vs 200+ fps with
			// readPixels), with loadPixels at the end of renderPickBuffer()
			pickColor = SMT.renderer.pixels[ x + y * SMT.renderer.width] & 0x00FFFFFF;
		else {
			ByteBuffer buffer = ByteBuffer.allocateDirect( 1 * 1 * SIZEOF_INT);

			pgl.readPixels(t.x, SMT.renderer.height - t.y, 1, 1, GL.GL_RGBA,
					GL.GL_UNSIGNED_BYTE, buffer);

			// get the first three bytes
			int r = buffer.get() & 0xFF;
			int g = buffer.get() & 0xFF;
			int b = buffer.get() & 0xFF;
			pickColor = (r << 16) + (g << 8) + (b);
			buffer.clear();
		}

		SMT.renderer.endPGL();

		if( zonesByPickColor.containsKey( pickColor)) {
			// if mapped it is either a Zone or null (background)
			Zone picked =  zonesByPickColor.get( pickColor);
			Zone current = picked;
			while (current != null){
				//find the first ancestor (including itself) with stealChildrensTouch and give it to that one
				if(current.stealChildrensTouch)
					return current;
				current = current.getParent();
			}
			return picked;
		}
		else {
			// not mapped means a bug in the pickDrawn colors, or maybe that BG_PICK_COLOR or a Zone got unmapped when it should'nt have
			// only show error in debug mode, since it is much to prevalent still to always show
			if( SMT.debug) 
				System.err.printf(
					"PickColor: %x doesn't match any known Zone's pickColor or the background, this indicates it was unmapped when it shouldn't be, or an incorrect color was drawn to the pickBuffer.",
					pickColor);
			return null;
		}
	}

	public void renderPickBuffer(){
		// make sure colorMode is correct for the pickBuffer
		SMT.renderer.colorMode( PConstants.RGB, 255);
		SMT.renderer.background( BG_PICK_COLOR, 255);
		SMT.sketch.drawForPickBuffer();
		SMT.renderer.flush();
		// If fast picking disabled, use loadPixels() which is really slow (max 70 fps on a high end card vs 200+ fps with readPixels) as a backup.
		PGL pgl = SMT.renderer.beginPGL();
		if ( ! SMT.fastPicking || pgl == null)
			SMT.renderer.loadPixels();
		SMT.renderer.endPGL();
	}
}
