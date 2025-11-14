import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class soundWave {

    private static volatile boolean playing = false;
    public static void main(String[] args) throws Exception {

        JFrame frame = new JFrame("Waveform Viewer");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800,600);
        frame.setLayout(new BorderLayout());

        JPanel channelPanel = new JPanel();
        channelPanel.setPreferredSize(new Dimension(frame.getWidth(), 150));
        frame.add(channelPanel, BorderLayout.NORTH);


        WaveformPanel waveformPanel = new WaveformPanel(new byte[0]);
        channelPanel.add(waveformPanel);

        JPanel channelSelectPanel = new JPanel();
        channelSelectPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 20, 5));
        channelPanel.add(channelSelectPanel);

        JButton channelButton = new JButton("1");
        channelButton.setFont(new Font("Arial", Font.BOLD, 18));

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 20, 5));

        JButton playButton = new JButton("Play");
        playButton.setFont(new Font("Arial", Font.BOLD, 18));


        JButton stopButton = new JButton("Stop");
        stopButton.setFont(new Font("Arial", Font.BOLD, 18));

        JButton squareWaveButton = new JButton("Square Wave");
        squareWaveButton.setFont(new Font("Arial", Font.BOLD, 18));

        JButton sineWaveButton = new JButton("Sine Wave");
        sineWaveButton.setFont(new Font("Arial", Font.BOLD, 18));

        buttonPanel.add(playButton);
        buttonPanel.add(stopButton);
        buttonPanel.add(squareWaveButton);
        buttonPanel.add(sineWaveButton);

        channelSelectPanel.add(channelButton);

        frame.add(buttonPanel, BorderLayout.SOUTH);


        stopButton.addActionListener((ActionEvent e) -> {
            playing = false;
        });
        

        playButton.addActionListener((ActionEvent e) -> {

            if (playing) return;
            playing = true;

            new Thread(() -> {
                try {
                    float sampleRate = 44100;
                    int chunkMs = 50;
                    int chunkSize = (int)(sampleRate * chunkMs / 1000);
                    int totalWaves = 5;
                    double durationSeconds = 5.0;
                    double frequency = totalWaves / durationSeconds;

                    AudioFormat format = new AudioFormat(sampleRate, 8, 1, true, false);
                    SourceDataLine line = AudioSystem.getSourceDataLine(format);
                    line.open(format);
                    line.start();

                    int displaySeconds = 15;
                    byte[] slidingBuffer = new byte[(int) (sampleRate * displaySeconds)];

                    int i = 0;
                    while (playing) {
                        byte[] buffer = new byte[chunkSize];

                        for (int j = 0; j < chunkSize; j++, i++) {
                            double angle = 2.0 * Math.PI * i * frequency / sampleRate;
                            byte sample = (byte)(Math.sin(angle) >= 0 ? 127 : -128);
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

