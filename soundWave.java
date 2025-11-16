import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.*;

public class soundWave {

    public static class Channel {
        private final WaveformPanel panel;
        private final WaveSettings settings;
        private volatile boolean playing = false;
        private Thread audioThread;


        public Channel(WaveformPanel panel){
            this.panel = panel;
            this.settings = new WaveSettings();
        }

        public WaveSettings getSettings() {
            return settings;
        }

        public WaveformPanel getPanel() {
            return panel;
        }

        public void start() {
            if (playing) return;
            playing = true;
            audioThread = new Thread(() -> runAudio());
            audioThread.start();
        }

        public void stop() {
            playing = false;
        }

        public void runAudio() {
            try {
                float sampleRate = 44100;
                int chunkMs = 50;
                int chunkSize = (int)(sampleRate * chunkMs / 1000);

                AudioFormat format = new AudioFormat(sampleRate, 8, 1, true, false);
                SourceDataLine line = AudioSystem.getSourceDataLine(format);
                line.open(format);
                line.start();

                int displaySeconds = 15;
                byte[] slidingBuffer = new byte[(int) (sampleRate * displaySeconds)];

                int i = 0;
                boolean beatOn = false;

                while (playing) {
                    byte[] buffer = new byte[chunkSize];

                    WaveSettings.WaveShape shape = settings.getShape();
                    double amplitude = settings.getAmplitude();
                    double frequency = settings.getFrequency();
                    double duty = settings.getPulseDuty();

                    double bpm = settings.getBPM();
                    int beatCounter = 0;
                    int beatLengthSamples = (int)(sampleRate * 0.1);
                    int samplesPerBeat = (int)(sampleRate * (60/bpm));

                    for (int j = 0; j < chunkSize; j++, i++) {

                        if ((i % samplesPerBeat) < beatLengthSamples){
                            beatOn = true;
                        } else {
                            beatOn = false;
                        }
   
                        double angle = 2.0 * Math.PI * i * frequency / sampleRate;
                        byte sample = 0;

                        if (beatOn) {
                        switch (shape) {

                            case SQUARE:
                                sample = (byte)((Math.sin(angle) >= 0 ? 127:-128) * amplitude);
                                break;
  

                            case SAW:
                                double saw = 2.0 * (angle / (2 * Math.PI) - 
                                Math.floor(0.5 + angle / (2 * Math.PI)));
                                sample = (byte)(saw * 127 * amplitude);
                                break;

                            case PULSE:
                                double phase = (angle / (2 * Math.PI)) % 1.0;
                                sample = (byte)((phase < duty ? 127 : -128) * amplitude);
                                break;

                            case NOISE:
                                sample = (byte)((Math.random() * 255 - 128) * amplitude);
                                break;
                                    
                            default:
                                sample = 0;
                        }
                    }

                        buffer[j] = sample;
                        slidingBuffer[i % slidingBuffer.length] = sample;
                    
                        beatCounter++;
                        if (beatCounter >= samplesPerBeat){
                            beatCounter = 0;
                        }
                    
                    }

                    panel.setSamples(slidingBuffer);
                    panel.repaint();

                    line.write(buffer , 0, buffer.length);
                }

                line.drain();
                line.stop();
                line.close();

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public static final class WaveSettings {
            enum WaveShape {SQUARE, SAW, PULSE, NOISE}

            private double frequency = 1.0;
            private double amplitude = 1.0;
            private double volume = 1.0;
            private WaveShape shape = WaveShape.SQUARE;
            private double pulseDuty = 0.25;
            private int bpm = 120;

            public double getFrequency() { return frequency; }
            public void setFrequency(double f) {frequency = f; }

            public double getAmplitude() { return amplitude; }
            public void setAmplitude(double a) {amplitude = a; }

            public double getVolume() { return volume; }
            public void setVolume(double v) {volume = v; }

            public WaveShape getShape() { return shape; }
            public void setShape(WaveShape s) {shape = s; }

            public double getPulseDuty() {return pulseDuty;}
            public void setPulseDuty(double d) {pulseDuty = Math.max(0.01, Math.min(0.99, d));}
        
            public double getBPM() {return bpm;}
            public void setBPM(int b) {bpm = b;}


        }

    static class ChannelPanel extends JPanel {
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setColor(Color.CYAN);
            g2.fillRect(0,0, getWidth(), getHeight());
        }
    }

    static class WaveformPanel extends JPanel {
        private byte[] samples;

        public WaveformPanel(byte[] samples) {
            this.samples = samples;
        }

        public void setSamples(byte[] samples) {
            this.samples = samples;
        }
    

        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            Graphics2D g2 = (Graphics2D) g;
            g2.setColor(Color.BLACK);
            g2.fillRect(0,0, getWidth(), getHeight());

            g2.setColor(Color.GREEN);
            g2.setStroke(new BasicStroke(3));

            if (samples == null || samples.length == 0) return;
            
            int mid = getHeight() / 5;
            int width = getWidth();
            int step = samples.length / getWidth();
            

            for (int i = 0; i < width -1; i++) {
                int sample1 = samples[i * step];
                int sample2 = samples[(i + 1) * step];

                float scale = 0.5f; // 50% of the available vertical space
                int y1 = mid - (int)(sample1 * mid / 128 * scale);
                int y2 = mid - (int)(sample2 * mid / 128 * scale);

                g.drawLine(i, y1, i+1, y2);
            }
        }
    }

    public static void main(String[] args) throws Exception {

        WaveformPanel[] waveformPanels = new WaveformPanel[] {
            new WaveformPanel(new byte[0]),
            new WaveformPanel(new byte[0]),
            new WaveformPanel(new byte[0]),
            new WaveformPanel(new byte[0])
        };

        Channel[] channels = new Channel[4];
        channels[0] = new Channel(waveformPanels[0]);
        channels[1] = new Channel(waveformPanels[1]);
        channels[2] = new Channel(waveformPanels[2]);
        channels[3] = new Channel(waveformPanels[3]);
    
        final int[] activeChannel = {0} ;

        JFrame frame = new JFrame("Waveform Viewer");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800,625);
        frame.setLayout(new BorderLayout());

        //Channel container

        ChannelPanel channelPanel = new ChannelPanel();
        channelPanel.setLayout(new BoxLayout(channelPanel, BoxLayout.Y_AXIS));
        channelPanel.setPreferredSize(new Dimension(750,600));

        //Channel 1
        JPanel channel1 = new JPanel();
        channel1.setPreferredSize(new Dimension(750,150));

        JPanel channelSelectPanel1 = new JPanel();
        JButton channelButton1 = new JButton("1");
        channelButton1.addActionListener(e -> activeChannel[0] = 0);
        waveformPanels[0].setPreferredSize(new Dimension(725,150));

        //Channel 2

        JPanel channel2 = new JPanel();
        channel2.setPreferredSize(new Dimension(750,150));

        JPanel channelSelectPanel2 = new JPanel();
        JButton channelButton2 = new JButton("2");
        channelButton2.addActionListener(e -> activeChannel[0] = 1);
        waveformPanels[1].setPreferredSize(new Dimension(725,150));

        //         //Channel 3
        JPanel channel3 = new JPanel();
        channel1.setPreferredSize(new Dimension(750,150));

        JPanel channelSelectPanel3 = new JPanel();
        JButton channelButton3 = new JButton("3");
        channelButton3.addActionListener(e -> activeChannel[0] = 2);
        waveformPanels[2].setPreferredSize(new Dimension(725,150));

        // //Channel 4

        JPanel channel4 = new JPanel();
        channel4.setPreferredSize(new Dimension(750,150));

        JPanel channelSelectPanel4 = new JPanel();
        JButton channelButton4 = new JButton("4");
        channelButton4.addActionListener(e -> activeChannel[0] = 3);
        waveformPanels[3].setPreferredSize(new Dimension(725,150));

        //Adding channels to channel panel

        channel1.add(channelSelectPanel1, BorderLayout.LINE_START);
        channel1.add(waveformPanels[0], BorderLayout.CENTER);
        channelSelectPanel1.add(channelButton1, BorderLayout.NORTH);
        channelPanel.add(channel1);

        channel2.add(channelSelectPanel2, BorderLayout.LINE_START);
        channel2.add(waveformPanels[1], BorderLayout.CENTER);
        channelSelectPanel2.add(channelButton2, BorderLayout.SOUTH);
        channelPanel.add(channel2);

        channel3.add(channelSelectPanel3, BorderLayout.LINE_START);
        channel3.add(waveformPanels[2], BorderLayout.CENTER);
        channelSelectPanel3.add(channelButton3, BorderLayout.SOUTH);
        channelPanel.add(channel3);

        channel4.add(channelSelectPanel4, BorderLayout.LINE_START);
        channel4.add(waveformPanels[3], BorderLayout.CENTER);
        channelSelectPanel4.add(channelButton4, BorderLayout.SOUTH);
        channelPanel.add(channel4);

        frame.add(channelPanel);

        //Button Section

        JPanel controlContainer = new JPanel();
        controlContainer.setLayout(new BoxLayout(controlContainer, BoxLayout.X_AXIS));
        controlContainer.setSize(800, 450);;
        frame.add(controlContainer, BorderLayout.SOUTH);

        //AMP and Freq

        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new GridLayout(4, 6, 10, 5));
        
        JButton amp6Button = new JButton("100");
        JButton amp5Button = new JButton("85");
        JButton amp4Button= new JButton("70");
        JButton amp3Button = new JButton("55");
        JButton amp2Button = new JButton("30");
        JButton amp1Button= new JButton("0");

        JButton freq1Button = new JButton("0.5hz");
        JButton freq2Button = new JButton("1hz");
        JButton freq3Button= new JButton("2hz");
        JButton freq4Button = new JButton("40hz");
        JButton freq5Button = new JButton("60hz");
        JButton freq6Button= new JButton("200hz");

        JButton bpm1Button = new JButton("30bpm");
        JButton bpm2Button = new JButton("60bpm");
        JButton bpm3Button = new JButton("90bpm");
        JButton bpm4Button = new JButton("120bpm");
        JButton bpm5Button = new JButton("150bpm");
        JButton bpm6Button = new JButton("180bpm");

        JButton playButton = new JButton("Play");
        JButton stopButton = new JButton("Stop");
        JButton squareWaveButton = new JButton("Square Wave");
        JButton pulseWaveButton = new JButton("Pulse Wave");
        JButton sawWaveButton = new JButton("Saw Wave");
        JButton noiseWaveButton = new JButton("Loop");

        controlPanel.add(amp1Button);
        controlPanel.add(amp2Button);
        controlPanel.add(amp3Button);
        controlPanel.add(amp4Button);
        controlPanel.add(amp5Button);
        controlPanel.add(amp6Button);

        controlPanel.add(freq1Button);
        controlPanel.add(freq2Button);
        controlPanel.add(freq3Button);
        controlPanel.add(freq4Button);
        controlPanel.add(freq5Button);
        controlPanel.add(freq6Button);

        controlPanel.add(bpm1Button);
        controlPanel.add(bpm2Button);
        controlPanel.add(bpm3Button);
        controlPanel.add(bpm4Button);
        controlPanel.add(bpm5Button);
        controlPanel.add(bpm6Button);

        controlPanel.add(playButton);
        controlPanel.add(stopButton);
        controlPanel.add(squareWaveButton);
        controlPanel.add(pulseWaveButton);
        controlPanel.add(sawWaveButton);
        controlPanel.add(noiseWaveButton);

        controlContainer.add(controlPanel);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridLayout(3,3,10,5));

        //Control buttons



        controlContainer.add(buttonPanel, BorderLayout.SOUTH);

        //Button functions

        playButton.addActionListener(e -> 
            channels[activeChannel[0]].start()
        );

        stopButton.addActionListener(e ->
            channels[activeChannel[0]].stop()
        );
        
        squareWaveButton.addActionListener( e -> 
            channels[activeChannel[0]].getSettings().setShape(WaveSettings.WaveShape.SQUARE)
        );

        pulseWaveButton.addActionListener( e -> 
            channels[activeChannel[0]].getSettings().setShape(WaveSettings.WaveShape.PULSE)
        
        );

        sawWaveButton.addActionListener( e -> 
            channels[activeChannel[0]].getSettings().setShape(WaveSettings.WaveShape.SAW)
        );

        noiseWaveButton.addActionListener( e -> 
            channels[activeChannel[0]].getSettings().setShape(WaveSettings.WaveShape.NOISE)
        );
        freq1Button.addActionListener( e -> 
            channels[activeChannel[0]].getSettings().setFrequency(0.5)
        );

        freq2Button.addActionListener( e -> 
            channels[activeChannel[0]].getSettings().setFrequency(1)
        );

        freq3Button.addActionListener( e -> 
            channels[activeChannel[0]].getSettings().setFrequency(2)
        );

        amp1Button.addActionListener( e -> 
            channels[activeChannel[0]].getSettings().setAmplitude(0)
        );

        amp2Button.addActionListener( e -> 
            channels[activeChannel[0]].getSettings().setAmplitude(0.5)
        );

        amp3Button.addActionListener( e -> 
            channels[activeChannel[0]].getSettings().setAmplitude(1)
        );

        bpm1Button.addActionListener(e ->
            channels[activeChannel[0]].getSettings().setFrequency(40)
        );

        bpm2Button.addActionListener(e ->
            channels[activeChannel[0]].getSettings().setFrequency(60)
        );

        bpm3Button.addActionListener(e ->
            channels[activeChannel[0]].getSettings().setFrequency(200)
        );

        bpm4Button.addActionListener(e ->
            channels[activeChannel[0]].getSettings().setBPM(120)
        );

        bpm5Button.addActionListener(e ->
            channels[activeChannel[0]].getSettings().setBPM(60)
        );

        bpm6Button.addActionListener(e ->
            channels[activeChannel[0]].getSettings().setBPM(180)
        );

        frame.setVisible(true);
    }

    public static void play (byte[] audioData, float sampleRate) throws Exception {
        AudioFormat format = new AudioFormat(sampleRate, 8, 1, true, false);
        SourceDataLine line = AudioSystem.getSourceDataLine(format);
        line.open(format);
        line.start();
        line.write(audioData, 0, audioData.length);
        line.drain();
        line.stop();
        line.close();
    }
}

