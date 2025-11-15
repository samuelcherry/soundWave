import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class soundWave {

    public static WaveSettings settings = new WaveSettings();

    private static volatile boolean playing = false;

    public static class WaveSettings {
        public enum WaveShape {SQUARE, SAW, PULSE, NOISE}

        private double frequency = 1.0;
        private double amplitude = 1.0;
        private double volume = 1.0;
        private WaveShape shape = WaveShape.SQUARE;
        private double pulseDuty = 0.25;

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
    }

    public static void main(String[] args) throws Exception {




        JFrame frame = new JFrame("Waveform Viewer");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800,300);
        frame.setLayout(new BorderLayout());

        //Channels

        ChannelPanel channelPanel = new ChannelPanel();
        channelPanel.setPreferredSize(new Dimension(frame.getWidth(), 150));
        channelPanel.setLayout(new BorderLayout());
        channelPanel.setSize(450, 150);

        JPanel channelSelectPanel = new JPanel();
        JButton channelButton = new JButton("1");

        WaveformPanel waveformPanel = new WaveformPanel(new byte[0]);
        waveformPanel.setSize(400, 400);

        channelPanel.add(channelSelectPanel, BorderLayout.LINE_START);
        channelPanel.add(waveformPanel, BorderLayout.CENTER);
        channelSelectPanel.add(channelButton);

        frame.add(channelPanel);

    
        //Button Section

        JPanel controlContainer = new JPanel();
        controlContainer.setLayout(new BoxLayout(controlContainer, BoxLayout.X_AXIS));
        controlContainer.setSize(800, 450);;
        frame.add(controlContainer, BorderLayout.SOUTH);

        //AMP and Freq

        JPanel ampFreqPanel = new JPanel();
        ampFreqPanel.setLayout(new GridLayout(2, 3, 10, 5));
        
        JButton amp3Button = new JButton("100");
        JButton amp2Button = new JButton("50");
        JButton amp1Button= new JButton("0");
        JButton freq3Button = new JButton("2hz");
        JButton freq2Button = new JButton("1hz");
        JButton freq1Button= new JButton("0.5hz");

        ampFreqPanel.add(amp3Button);
        ampFreqPanel.add(amp2Button);
        ampFreqPanel.add(amp1Button);
        ampFreqPanel.add(freq3Button);
        ampFreqPanel.add(freq2Button);
        ampFreqPanel.add(freq1Button);

        controlContainer.add(ampFreqPanel, BorderLayout.WEST);
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridLayout(2,3,10,5));

        //Control buttons

        JButton playButton = new JButton("Play");
        JButton stopButton = new JButton("Stop");
        JButton squareWaveButton = new JButton("Square Wave");
        JButton pulseWaveButton = new JButton("Pulse Wave");
        JButton sawWaveButton = new JButton("Saw Wave");
        JButton noiseWaveButton = new JButton("Noise Wave");

        buttonPanel.add(playButton);
        buttonPanel.add(stopButton);
        buttonPanel.add(squareWaveButton);
        buttonPanel.add(pulseWaveButton);
        buttonPanel.add(sawWaveButton);
        buttonPanel.add(noiseWaveButton);

        controlContainer.add(buttonPanel, BorderLayout.SOUTH);

        //Button functions
        stopButton.addActionListener((ActionEvent e) -> {
            playing = false;
        });
        
        squareWaveButton.addActionListener( e -> 
            settings.setShape(WaveSettings.WaveShape.SQUARE)
        );

        pulseWaveButton.addActionListener( e -> 
            settings.setShape(WaveSettings.WaveShape.PULSE)
        );

        sawWaveButton.addActionListener( e -> 
            settings.setShape(WaveSettings.WaveShape.SAW)
        );

        noiseWaveButton.addActionListener( e -> 
            settings.setShape(WaveSettings.WaveShape.NOISE)
        );
        freq1Button.addActionListener( e -> 
            settings.setFrequency(0.5)
        );

        freq2Button.addActionListener( e -> 
            settings.setFrequency(1)
        );

        freq3Button.addActionListener( e -> 
            settings.setFrequency(2)
        );

        amp1Button.addActionListener( e -> 
            settings.setAmplitude(0)
        );

        amp2Button.addActionListener( e -> 
            settings.setAmplitude(0.5)
        );

        amp3Button.addActionListener( e -> 
            settings.setAmplitude(1)
        );


        playButton.addActionListener((ActionEvent e) -> {

            if (playing) return;
            playing = true;

            new Thread(() -> {
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
                    while (playing) {
                        byte[] buffer = new byte[chunkSize];

                        WaveSettings.WaveShape shape = settings.getShape();
                        double amplitude = settings.getAmplitude();
                        double frequency = settings.getFrequency();
                        double duty = settings.getPulseDuty();

                        for (int j = 0; j < chunkSize; j++, i++) {
                            double angle = 2.0 * Math.PI * i * frequency / sampleRate;
                            byte sample;

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

                            buffer[j] = sample;

                            int pos = i % slidingBuffer.length;
                            slidingBuffer[pos] = sample;
                        }

                        waveformPanel.setSamples(slidingBuffer);
                        waveformPanel.repaint();

                        line.write(buffer , 0, buffer.length);
                    }

                    line.drain();
                    line.stop();
                    line.close();

                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }).start();
        });

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
}

