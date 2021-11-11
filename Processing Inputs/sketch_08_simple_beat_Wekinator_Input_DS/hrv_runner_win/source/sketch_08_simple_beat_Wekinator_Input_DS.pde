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

import oscP5.*;
import netP5.*;

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
float BPM = 0.0;
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

void setup() {
  size(712, 586);
  //font = loadFont("SansSerif-10.vlw");
  //textFont(font, 10);
  oscP5 = new OscP5(this, 11999);
  destination = new NetAddress("127.0.0.1", 6448);
  bitalino = new Bitalino2(this, PORT);
  bitalino.start(10); // data acquisition with 10 samples / second
  // bitalino.start(100, new int [] { 0, 1, 2, 3, 4, 5 }, 0x2); // emulation
}

void draw() {
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

  BPM = (1.0/PulseInterval) * 60.0 * 1000;
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

void sendOsc(float [] values, NetAddress dest) { 
  OscMessage msg = new OscMessage("/wek/inputs");
  for (float value : values) { 
    msg.add(value);
  }
  oscP5.send(msg, dest);
}
