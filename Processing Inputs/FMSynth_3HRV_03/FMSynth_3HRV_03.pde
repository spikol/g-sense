// This demo allows wekinator to control FM Synthesis
// Based on code by Damien Di Fede (Minim FM example)
// Modified by Rebecca Fiebrink to work with Wekinator

// import everything necessary to make sound.
import ddf.minim.*;
import ddf.minim.ugens.*;

// create all of the variables that will need to be accessed in
// more than one methods (setup(), draw(), stop()).
Minim minim;
AudioOutput out;
PGraphics pg;// blur circle

// the Oscil we use for modulating frequency.
Oscil fm;

//Parameters to control via wekinator:
float modulateAmount = 0.1; //0 to 500 ; 132
float modulateFrequency = 0.1; // 0 to 500 ; 50
float offset = 0.4; //0 to 500; 200
// COLOR
float myHue;
float c; //circle alpha
float zoom;

//Necessary for OSC communication with Wekinator:
import oscP5.*;
import netP5.*;
OscP5 oscP5;
NetAddress dest;


void setup() {
  //Initialize OSC communication
  oscP5 = new OscP5(this,12000); //listen for OSC messages on port 12000 (Wekinator default)
  dest = new NetAddress("127.0.0.1",6448); //send messages back to Wekinator on port 6448, localhost (this machine) (default)
  
  // initialize the drawing window
  colorMode(HSB);
  size(1080, 568, P3D );
  pg = createGraphics(1080, 568);
  myHue = 120;

  // initialize the minim and out objects
  minim = new Minim( this );
  out   = minim.getLineOut();
  
  // make the Oscil we will hear.
  // arguments are frequency, amplitude, and waveform
  Oscil wave = new Oscil( 200, 0.8, Waves.SINE );
  // make the Oscil we will use to modulate the frequency of wave.
  // the frequency of this Oscil will determine how quickly the
  // frequency of wave changes and the amplitude determines how much.
  // since we are using the output of fm directly to set the frequency 
  // of wave, you can think of the amplitude as being expressed in Hz.
  fm   = new Oscil( 10, 2, Waves.SINE );
  // set the offset of fm so that it generates values centered around 200 Hz
  fm.offset.setLastValue( offset * 500 );
  // patch it to the frequency of wave so it controls it
  fm.patch( wave.frequency );
  // and patch wave to the output
  wave.patch( out );
  
}

void draw() {
// erase the window to black
  background(myHue,120,160);
  //circles
  pg.beginDraw();
  pg.noStroke();
  color c = color(0, 126, 255, 102);
  pg.fill(c);
  pg.ellipse(560, 180, zoom, zoom);
  float value = alpha(c);  // Sets 'value' to 102
  pg.fill(value);
  pg.filter(BLUR, 4);
  pg.endDraw();
  image(pg, 0, 0);
  //circle(560, 240, 240);
 
  
  // draw using a white stroke
  stroke(255);
  strokeWeight(4); 
  // draw the waveforms
  for( int i = 0; i < out.bufferSize() - 1; i++ )
  {
    // find the x position of each buffer value
    float x1  =  map( i, 0, out.bufferSize(), 0, width );
    float x2  =  map( i+1, 0, out.bufferSize(), 0, width );
    // draw a line from one buffer position to the next for both channels
  // line( x1, 150 + out.left.get(i)*50, x2, 150 + out.left.get(i+1)*50);
   line( x1, 450 + out.right.get(i)*50, x2, 450 + out.right.get(i+1)*50);
  }  
  
  //fill(0, 255, 0);
  text( "Use 3 continuous Wekinator outputs", 5, 15 );
  text( "Send them to to port 12000 with OSC message /wek/outputs", 5, 30 );
  text( "Modulation frequency: " + fm.frequency.getLastValue(), 5, 45 );
  text( "Modulation amplitude: " + fm.amplitude.getLastValue(), 5, 60 );
  text( "Modulation offset: " + offset, 5, 75);  
}

// we can change the parameters of the frequency modulation Oscil
// in real-time using the mouse.
void mouseMoved()
{
  modulateAmount = map( mouseY, 0, height, 0, 1 );
   modulateFrequency = map( mouseX, 0, width, 0, 1 );
   updateFM();
   sendOsc();
}

void updateFM() {
   fm.setFrequency(modulateFrequency * 500 );
   fm.offset.setLastValue(offset * 1000);
   fm.setAmplitude(modulateAmount * 500);
  
    myHue = map(modulateAmount, 0, 1, 0, 255);
     println("MF" + myHue);
    myHue= int(myHue);
    c = int(myHue);
    zoom = int(myHue*4);//*10
     //println("MF" + myHue);
    //background(myHue, 255, 255);
}

//This is called automatically when OSC message is received
void oscEvent(OscMessage theOscMessage) {
 if (theOscMessage.checkAddrPattern("/wek/outputs")==true) {
     if(theOscMessage.checkTypetag("fff")) { //Now looking for 3 parameters
        float receivedModulation = theOscMessage.get(0).floatValue(); //get this parameter
        modulateFrequency = receivedModulation;
        
        float receivedAmount = theOscMessage.get(1).floatValue(); //get 2nd parameter
        modulateAmount = receivedAmount;
        
        float receivedOffset = theOscMessage.get(2).floatValue(); //get third parameters
        if (receivedOffset < 0) {
          receivedOffset = 0;
        }
        offset = receivedOffset;
        
        //Now use these params
        updateFM();
        
        println("Received new params value from Wekinator");  
      } else {
        println("Error: unexpected params type tag received by Processing");
      }
 }
}

//Sends current values to Wekinator (alternative to controlling via wekinator GUI)
void sendOsc() {
  OscMessage msg = new OscMessage("/wekinator/control/outputs");
  msg.add(modulateAmount); //Now send values back to wekinator, in order
  msg.add(modulateFrequency);
  msg.add(offset);
  oscP5.send(msg, dest);
}
