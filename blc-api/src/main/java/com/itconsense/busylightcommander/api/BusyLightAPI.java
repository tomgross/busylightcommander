package com.itconsense.busylightcommander.api;

import org.hid4java.HidDevice;
import org.hid4java.HidException;
import org.hid4java.HidManager;
import org.hid4java.HidServices;
import org.hid4java.HidServicesListener;
import org.hid4java.HidServicesSpecification;
import org.hid4java.ScanMode;
import org.hid4java.event.HidServicesEvent;

import java.lang.Math;

public class BusyLightAPI implements HidServicesListener {

    //vendor IDs
    public enum Vendor { PLENOM }
    private static int[] vendors = new int[]{ 0x27BB };

    //product IDs
    public enum Product { PRODUCT_OMEGA_ID, PRODUCT_ALPHA_ID, PRODUCT_UC_ID, PRODUCT_KUANDO_BOX_ID, PRODUCT_BOOTLOADER_ID}
    private static int[] products = new int[]{ 0x3BCD, 0x3BCA, 0x3BCB, 0x3BCC, 0x3BC0 };

    //colors
    public enum BLColor { RED, GREEN, BLUE, ORANGE, YELLOW, PINK, AQUA, WHITE, INDIGO, VIOLET }
    private static short[][] colors = new short[][] {
            {0x64, 0x00, 0x00},
            {0x00, 0x64, 0x00},
            {0x00, 0x00, 0x64},
            {0x64, 0x32, 0x00},
            {0x64, 0x64, 0x00},
            {0x64, 0x00, 0x64},
            {0x00, 0x64, 0x64},
            {0x64, 0x64, 0x64},
            {29,0,51},
            {93,51,93}
    };

    //sounds
    public enum Ringtone { TONE_RISING, TONE_PHONE, TONE_SIMON, TONE_ALTERNATIVE, TONE_CLASSIC, TONE_ALIEN, TONE_OFFICE, TONE_LIVEWIRE, TONE_OLD, TONE_TRON, TONE_DISCO }
    private static short[] ringtones = new short[]{ 0b10100011, 0b11000011,0b10010011, 0b10001011, 0b10110011, 0b10011011 , 0b10111011, 0b11101011, 0b11001011, 0b11010011, 0b10101011 };

    private static final int PACKET_LENGTH = 64;

    private HidServices hidServices;
    private HidDevice hidDevice;
    private boolean bSound = false;
    private Ringtone ringTone = Ringtone.TONE_RISING; //default
    private short volume = 3;
    private KeepAliveThread kaThread;

    public BusyLightAPI() {
        kaThread = new KeepAliveThread("kat");
        initHidServices();
    }

    public static void main(String[] args) throws HidException, InterruptedException {

        BusyLightAPI light = new BusyLightAPI(); //my light
        light.initDevice(Vendor.PLENOM, Product.PRODUCT_OMEGA_ID, null);

        //test
        light.rainbow();
        Thread.sleep(3000);

        light.stopLight();
        light.shutdown();
    }

    private void checkOpen() {
        if (kaThread != null && kaThread.isAlive())
            kaThread.interrupt();

        if (hidDevice == null) {
            System.err.println("Error- HID device is null");
            return;
        }

        // Ensure device is open after an attach/detach event
        if (!hidDevice.isOpen()) {
            hidDevice.open();
        }
    }

    private void _send(short[] message) {
        if (sendBytes(message)) {
            //keep alive
            if (kaThread != null && !kaThread.isAlive()) {
                kaThread.interrupt();
                kaThread = new KeepAliveThread("kat");
                kaThread.start();
            }
        }

    }

    private void _steadyColor(short[] thecolor) {

        checkOpen();

        short soundByte = 0x80; //off
        if (bSound) {
            soundByte = ringtones[ringTone.ordinal()];
            //set volume
            soundByte = (short) ((soundByte & 0xF8) + volume);
        }

        short[] message = new short[]{
                0x11, 0x00,thecolor[0], thecolor[1], thecolor[2], 0xFF, 0x00, soundByte,  //step 0
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,  //step 1
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,  //step 2
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0xFF, 0xFF, 0xFF, 0xFF, 0x06, 0x93  //last two bytes are the MSB and LSB of the 16-bit checksum
        };
        _send(message);
    }


    public void steadyColor(BLColor c) {
        short[] thecolor = colors[c.ordinal()];
        _steadyColor(thecolor);
    }

    //takes a standard HEX RGB color, converts it to PWM
    public void steadyColor(int r, int g, int b) {

        short[] thecolor = convertHexToPWM(r,g,b);
        _steadyColor(thecolor);

    }

    private void keepAlive() {
        //keeps alive for max 11 seconds; send again to extend duration

        checkOpen();

        short[] message = new short[]{
                0x8F, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,  //step 0
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,  //step 1
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,  //step 2
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0xFF, 0xFF, 0xFF, 0xFF, 0x00, 0x00  //last two bytes are the MSB and LSB of the 16-bit checksum
        };

        if (!sendBytes(message)) {
            System.err.println("keepAlive failed");
        }

    }

    public void stopLight() {
        checkOpen();

        short[] message = new short[]{
                0x10, 0x01, 0x00, 0x00, 0x00, 0x01, 0x00, 0x80,  //step 0
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,  //step 1
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,  //step 2
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0xFF, 0xFF, 0xFF, 0xFF, 0x06, 0x93  //last two bytes are the MSB and LSB of the 16-bit checksum
        };

        if (!sendBytes(message)) {
            System.err.println("stop failed");
        }
    }

    private void _blinkColor(short[] thecolor, int timeOn, int timeOff) {
        checkOpen();

        // timeOn and timeOff must be in the range 0 - 255 inclusive
        timeOn = Math.min(timeOn, 255);
        timeOn = Math.max(timeOn, 0);

        timeOff = Math.min(timeOff, 255);
        timeOff = Math.max(timeOff, 0);

        short soundByte = 0x80; //off
        if (bSound) {
            soundByte = ringtones[ringTone.ordinal()];
            //set volume
            soundByte = (short) ((soundByte & 0xF8) + volume);
        }

        short[] message = new short[]{
                0x11, 0x01, thecolor[0], thecolor[1], thecolor[2], (short)timeOn, 0x00, soundByte,
                0x10, 0x01, 0x00, 0x00, 0x00, (short)timeOff, 0x00, 0xA0,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0xFF, 0xFF, 0xFF, 0xFF, 0x06, 0x93  //last two bytes are the MSB and LSB of the 16-bit checksum
        };
        _send(message);
    }

    //time on and time off are in tenths of a second
    public void blinkColor(BLColor c, int timeOn, int timeOff) {
        short[] thecolor = colors[c.ordinal()];
        _blinkColor(thecolor, timeOn, timeOff);

    }

    //time on and time off are in tenths of a second
    public void blinkColor(int r, int g, int b, int timeOn, int timeOff) {
        short[] thecolor = convertHexToPWM(r,g,b);
        _blinkColor(thecolor, timeOn, timeOff);
    }

    public void initHidServices() throws HidException {
        // Configure to use custom specification
        HidServicesSpecification hidServicesSpecification = new HidServicesSpecification();
        hidServicesSpecification.setAutoShutdown(true);
        hidServicesSpecification.setScanInterval(500);
        hidServicesSpecification.setPauseInterval(5000);
        hidServicesSpecification.setScanMode(ScanMode.SCAN_AT_FIXED_INTERVAL_WITH_PAUSE_AFTER_WRITE);

        // Get HID services using custom specification
        hidServices = HidManager.getHidServices(hidServicesSpecification);
        hidServices.addHidServicesListener(this);

        // Start the services
        hidServices.start();
    }

    public boolean initDevice(Vendor v, Product p, String serialNo) throws HidException {
        // Open the device device by Vendor ID and Product ID with wildcard serial number
        hidDevice = hidServices.getHidDevice(vendors[v.ordinal()], products[p.ordinal()], serialNo);
        if (hidDevice == null) {
            System.err.println("Error getting HID device: " + v.toString() + " , " + p.toString());
            return false;
        }
        return true; //device successfully initialized
    }

    public void ping() {
        new PingThread("pingthread").start();
    }

    public void rainbow() {
        long ms = 75;

        try {
            steadyColor(BLColor.RED); Thread.sleep(ms);
            steadyColor(BLColor.ORANGE); Thread.sleep(ms);
            steadyColor(BLColor.YELLOW); Thread.sleep(ms);
            steadyColor(BLColor.GREEN); Thread.sleep(ms);
            steadyColor(BLColor.BLUE); Thread.sleep(ms);
            steadyColor(BLColor.INDIGO); Thread.sleep(ms);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        stopLight();
    }

    public void shutdown() {
        if (kaThread != null) {
            if (kaThread.isAlive())
                kaThread.interrupt();
            kaThread = null;
        }
        if (hidDevice != null)
            hidDevice.close();
        if (hidServices != null)
            hidServices.shutdown();
    }

    @Override
    public void hidDeviceAttached(HidServicesEvent event) {
        //event called when a HID device is attached
    }

    @Override
    public void hidDeviceDetached(HidServicesEvent event) {
        //event called when a HID device is detached
    }

    @Override
    public void hidFailure(HidServicesEvent event) {
        //event called when a HID device has a failure
    }

    public boolean isSoundEnabled() {
        return bSound;
    }

    public void setSoundEnabled(boolean bSound) {
        this.bSound = bSound;
    }

    public Ringtone getRingTone() {
        return ringTone;
    }

    public void setRingTone(Ringtone ringTone) {
        this.ringTone = ringTone;
    }

    public short getVolume() {
        return volume;
    }

    public void setVolume(short volume) {
        if (volume > 7 || volume < 0) {
            System.err.println("Error - volume must be in the range 0-7.");
            volume = 3;
        }
        this.volume = volume;
    }

    public static short[] convertHexToPWM(int r, int g, int b) {
        short[] ret = new short[]{0,0,0};

        if (r < 0 || r > 255 || g < 0 || g > 255 || b < 0 || b > 255) {
            System.err.println("jbusylightapi:convertHexToPWM() - error: invalid RGB value(s): " + r + " , " + g + " , " + b);
            return ret;
        }

        ret[0] = (short)Math.round( (r / 255.0) * 100 );
        ret[1] = (short)Math.round( (g / 255.0) * 100 );
        ret[2] = (short)Math.round( (b / 255.0) * 100 );

        return ret;
    }

    public boolean sendBytes(short[] message) {

        if (message == null) {
            System.err.println("message is null");
            return false;
        }

        if (message.length != PACKET_LENGTH) {
            System.err.println("message is not length " + PACKET_LENGTH);
            return false;
        }

        //calculate checksum
        int checksum = 0;
        for (int i=0; i < 62; i++)
            checksum += message[i];

        //add checksum value
        int msb = checksum >> 8;
        int lsb = checksum & 0x00FF;
        message[62] = (short)msb;
        message[63] = (short)lsb;

        //convert to byte array
        byte[] message2 = new byte[message.length];
        for (int i=0; i < message.length; i++)
            message2[i] = (byte)message[i];

        int val = hidDevice.write(message2, PACKET_LENGTH, (byte) 0x00);
        if (val >= 0) {
            //good
            return true;
        } else {
            System.err.println("error: " + hidDevice.getLastErrorMessage());
            return false;
        }
    }

    class PingThread extends Thread {
        public PingThread(String name) {
            super(name);
        }
        @Override
        public void run() {
            steadyColor(BLColor.GREEN);
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            stopLight();
        }
    }

    class KeepAliveThread extends Thread {
        private static final int FREQUENCY_SECS = 8;
        private boolean bAlive;
        KeepAliveThread(String name) {
            super(name);
            bAlive = true;
        }
        @Override
        public void run() {
            while (bAlive) {
                keepAlive();
                try {
                    Thread.sleep(FREQUENCY_SECS * 1000);
                } catch (InterruptedException e) {
                    bAlive = false;
                }
            }
        }
    }
}
