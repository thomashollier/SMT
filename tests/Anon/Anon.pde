//standard library imports


//SMT library imports
import vialab.SMT.*;

//constants
int display_width = 1200;
int display_height = 800;
int display_halfWidth;
int display_halfHeight;
int fps_limit = 60;
//other


//main functions
void setup(){
	display_halfWidth = display_width / 2;
	display_halfHeight = display_height / 2;
	//processing window setup
	frameRate( fps_limit);
	size( display_width, display_height, P3D);
	SMT.init( this, TouchSource.AUTOMATIC);

	//stuff
	Zone asdf = new Zone( 120, 10, 100, 100){
		public void drawImpl(){
			pushStyle();
			fill( 180, 100, 100);
			rect( 0, 0, 100, 100, 5);
			popStyle();
		}
		public void pickDrawImpl(){
			rect( 0, 0, 100, 100, 5);
		}
		public void touchImpl(){
			rst();
		}
	};
	SMT.add( asdf);
	SMT.add( new Zone( 10, 10, 100, 100));
	//SMT.add( new SwipeKeyboard());
}

void draw(){
	//draw background
	background( 50, 50, 50);
}
