import processing.core.*; 
import processing.data.*; 
import processing.event.*; 
import processing.opengl.*; 

import oscP5.*; 
import netP5.*; 
import java.util.Arrays; 
import processing.serial.*; 

import java.util.HashMap; 
import java.util.ArrayList; 
import java.io.File; 
import java.io.BufferedReader; 
import java.io.PrintWriter; 
import java.io.InputStream; 
import java.io.OutputStream; 
import java.io.IOException; 

public class sketch_08_simple_beat_Wekinator_Input_DS extends PApplet {

// BITalino helper class for Processing
// Example_08a_Wekinator_Input
// BITalino (r)evolution
// by @crcdng
// oscP5 library by Andreas Schlegel
// Wekinator by Rebecca Fiebrink http://www.wekinator.org
// Wekinator project resources are in the data folder
// run parallel with Example_08b_Wekinator_Output

// BITalino: EDA, EEG => 
// Wekinator_Input Sketch => 
// Port 6448 /wek/inputs Wekinator All continous (Regression) => 
// Record for given outputs, e.g. 0 when calm and 1 when agitated, train, then run => 
// Port 12000 /wek/outputs => 
// Wekinator_Output Sketch [0..1] 




final int ECG = 2;  // Electrocardigram activity signal (skin, channel 2)
final int EDA = 0;  // Electrodermal activity signal (skin, channel 3)
//final int EEG = 3;  // Electroencephalogram signal (brainz, channel 4)

OscP5 oscP5;
NetAddress destination;

Bitalino bitalino;
final int PORT = 0; // the index of the BITalino port displayed in the console
int data[] = new int[6]; // data of the 6 acquisition channels
float sendValues[] = new float[2]; //sending 2 channels
// hrv
int UpperThreshold = 510; //peak
int LowerThreshold = 500; //false reading e.g. 0
int reading = 0;
float BPM = 0.0f;
boolean IgnoreReading = false;
boolean FirstPulseDetected = false;
boolean AverageMaker =false;
long FirstPulseTime = 0;
long SecondPulseTime = 0;
long PulseInterval = 0;
int beats=0;
int[] beatArray = new int[4];
int RateSize=3;
int rateSpot=0;
int beatAvg = 0; //int without sqrt 

public void setup() {
  
  //font = loadFont("SansSerif-10.vlw");
  //textFont(font, 10);
  oscP5 = new OscP5(this, 11999);
  destination = new NetAddress("127.0.0.1", 6448);
  bitalino = new Bitalino2(this, PORT);
  bitalino.start(10); // data acquisition with 10 samples / second
  // bitalino.start(100, new int [] { 0, 1, 2, 3, 4, 5 }, 0x2); // emulation
}

public void draw() {
  background(0);
  data = bitalino.receive(); // read a sample of raw data
  int ecg = data[ECG];
  int eda = data[EDA];
  //int eeg = data[EEG];

  // visualize the values
  float yEca = map(ecg, 0, 1024, 0, height); //map 0 to 1024
  fill(12, 18, 244);
  rect(10, height - yEca, 30, yEca); // visualize

  //float yEda = map(eda, 0, 1024, 0, height);
  //fill(12, 18, 244);
  //rect(10, height - yEda, 30, yEda); // visualize
  //float yEeg = map(eeg, 0, 1024, 0, height);
  //fill(12, 244, 18);
  //rect(60, height - yEeg, 30, yEeg); 

  //HRB bpm // reading = ecg; 
  if (ecg > UpperThreshold && IgnoreReading == false) {
    if (FirstPulseDetected == false) {
      FirstPulseTime = millis();
      FirstPulseDetected = true;
    } else {
      SecondPulseTime = millis();
      PulseInterval = SecondPulseTime - FirstPulseTime;
      FirstPulseTime = SecondPulseTime;
      AverageMaker = true;
    }
    IgnoreReading = true;
  }
  if (ecg < LowerThreshold) {
    IgnoreReading = false;
  }  

  BPM = (1.0f/PulseInterval) * 60.0f * 1000;
  beats= round(BPM);
  if (AverageMaker == true ) {
    beatArray[rateSpot++] = beats; //Store this reading in the array
    rateSpot %= RateSize;
    //Take average of readings

    AverageMaker= false;
  }
  // Average BPM
  for (int x = 0; x < RateSize; x++) {
    beatAvg += beatArray[x];
    println(beatAvg);
    beatAvg /= RateSize;
    //beatAvg = beatAvg/(beatAvg);
  }
  textSize(26);
  text("Raw "+ ecg, 220, 30);
  text("BPM " + BPM, 220, 65);
  text("Beat Average " + beatAvg, 220, 95);
  // text("Beatarray " + beatArray[]);
  text("Pulse Intervat ms " + PulseInterval, 220, 150); // send raw EDA, EEG values to Wekinator
  sendValues[0] = beatAvg; //last minute 
  sendValues[1] = PulseInterval;
  //sendValues[1] = eeg;
  sendOsc(sendValues, destination);
}

public void sendOsc(float [] values, NetAddress dest) { 
  OscMessage msg = new OscMessage("/wek/inputs");
  for (float value : values) { 
    msg.add(value);
  }
  oscP5.send(msg, dest);
}
// BITalino helper class for Processing //<>//
// by @crcdng 

  //<>//


enum Mode {
  IDLE, ACQUIRE, EMULATE
}

class Bitalino2 extends Bitalino {

  Bitalino2(PApplet applet, int portnumber) {  
    super(applet, portnumber);
  }

  Bitalino2(PApplet applet, String portname) {  
    super(applet, portname);
  }

  public void pwm() { 
    pwm(100);
  }
  public void pwm(int analog) { 
    send(0xA3); // set analog output
    send(analog); // 0 <= analog <= 255
  }
  
  // 0, 0 > 10110011
  // 1, 0 > 10110111 LED
  // 0, 1 > 10111011 Buzzer
  // 1, 1 > 10111011 LED + Buzzer 
  public void trigger(int [] digital) { // expected format: array [1/0,1/0]
    int output = 0x0;
    for (int i = 0; i < digital.length; i++) {
      if (digital[i] == 1) { 
        output = output | (1 << i);
      }
    }
    send((output << 2) | 0xB3); // set digital outputs 1 0 1 1 O1 O2 1 1
  }
}

abstract class Bitalino {
  int [] buffer = { 0, 0, 0, 0, 0, 0, 0, 0 };
  PrintWriter file;  
  int index = 0;
  int numChannels = 6;
  Serial port;
  Integer [] samplingRates = { 1, 10, 100, 1000 };
  Mode mode;
  int [] values = new int[numChannels];
  
  Bitalino(PApplet applet, int portnumber) {  
    this(applet, Serial.list()[portnumber]);
    println("Bitalino: Available ports:");    
    printArray(Serial.list());
  }
  
  Bitalino(PApplet applet, String portname) { 
    try {
      port = new Serial(applet, portname, 115200);
    } catch (Exception e) {
      println("Bitalino: Could not open a connection to the BITalino at port: " + portname);    
      println("Exiting...");
      exit();
    } 
    
    registerMethod("dispose", applet);  // CAREFUL registerMethod is undocumented
    mode = Mode.IDLE;
  }

  public void battery() { 
    battery(0);
  }
  public void battery(int value) {
    if (!isIdle()) { throw new Error("Bitalino: battery() can only be called in idle mode"); }
    send(value << 2 | 0x00);
  }

  public boolean checkCRC (int [] buf) {
    int crc = buf[7] & 0x0F; 
    buf[7] &= 0xF0;  // clear CRC bits in frame
    int x = 0;
    for (int i = 0; i < buf.length; i++) {
      for (int bit = 7; bit >= 0; bit--) {
        x <<= 1;
        if ((x & 0x10) > 0)  x = x ^ 0x03;
        x ^= ((buf[i] >> bit) & 0x01);
      }
    }
    return (crc == (x & 0x0F));
  }
  
  public void close() { 
    port.stop();
  }
    
  public boolean isAcquiring() { return mode == Mode.ACQUIRE; }
  public boolean isEmulating() { return mode == Mode.EMULATE; }
  public boolean isIdle() { return mode == Mode.IDLE; }
  
  public int [] read() { 
    return read(100);
  }
  public int [] read(int numSamples) {  
    throw new Error("Bitalino: not implemented"); 
    // return null;
  }
  
  public int [] receive () {
    while (port.available () > 0) {
      buffer[index] = port.read(); // read an unsigned byte 0..255
      if ((index == (buffer.length - 1)) && checkCRC(buffer)) {
        values = toRawValues(buffer);
      }
      index = (index + 1) % buffer.length;
    }
    return values;
  }
  
  public int [] record (int [] data) {
    file.println(join(str(data), ","));
    return data;
  }

  public void send(int data) {
    try {
      port.write(data);
    } catch (Exception e) {}
  } 

  public void start() { 
    start(1000, new int [] { 0, 1, 2, 3, 4, 5 }, 0x1);
  }
  public void start(int samplingRate) { 
    start(samplingRate, new int [] { 0, 1, 2, 3, 4, 5 }, 0x1);
  }
  public void start(int samplingRate, int [] acquisitionChannels, int acquisitionMode) { 
    int commandSRate = Arrays.asList(samplingRates).indexOf(samplingRate);
    send((commandSRate << 6) | 0x03); // set sampling rate S S 0 0 0 0 1 0
    delay(10);  
    int channels = 0x0;
    for (int c : acquisitionChannels) {
      channels = channels | (1 << c);
    }
    send((channels << 2) | acquisitionMode); // set live (0x1) or emulation (0x2) mode with channels A6 A5 A4 A3 A2 A1 M M
    if (acquisitionMode == 1) { 
      mode = Mode.ACQUIRE; 
    } else if (acquisitionMode == 2) { 
      mode = Mode.EMULATE; 
    }
  }
  
  public void startRecording () { 
    String filename = "bitalino-data-" + nf(year(), 4) + "-" + nf(month(), 2) + "-" + 
    nf(day(), 2) + "-" + nf(hour(), 2) + "-" + nf(minute(), 2) + 
    "-" + String.valueOf(second()) + ".txt";
    startRecording(dataPath(filename)); // CAREFUL dataPath is undocumented
  } 
  public void startRecording (String path) {
    file = createWriter(path);
  }

  public void state() { 
    throw new Error("Bitalino: not implemented"); 
    // return null;
  }

  public void stopRecording () {
    file.flush(); 
    file.close();
  }
  
  public void stop() { 
    send(0x0); // stop acquisition and set idle mode
    mode = Mode.IDLE;
  }
  
 public int [] toRawValues(int [] data) {  
    values[0] = ((data[6] & 0x0F) << 6) | (data[5] >> 2);
    values[1] = ((data[5] & 0x03) << 8) | (data[4]);
    values[2] = ((data[3]       ) << 2) | (data[2] >> 6);
    values[3] = ((data[2] & 0x3F) << 4) | (data[1] >> 4);
    values[4] = ((data[1] & 0x0F) << 2) | (data[0] >> 6);
    values[5] = ((data[0] & 0x3F));  
    return values;
  }
  
  public abstract void trigger(int [] digital);

  public void version() {
    if (!isIdle()) { throw new Error("Bitalino: version() can only be called in idle mode"); }
    send(0x7); // ask for version. the result is assembled in the serialEvent(Serial port) callback.    
  }
}
  public void settings() {  size(712, 586); }
  static public void main(String[] passedArgs) {
    String[] appletArgs = new String[] { "sketch_08_simple_beat_Wekinator_Input_DS" };
    if (passedArgs != null) {
      PApplet.main(concat(appletArgs, passedArgs));
    } else {
      PApplet.main(appletArgs);
    }
  }
}
