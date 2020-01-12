package com.itconsense.busylightcommander.cli;


import picocli.CommandLine;

import static java.lang.Thread.sleep;
import static picocli.CommandLine.Command;
import static picocli.CommandLine.Option;

import java.lang.Math;
import com.itconsense.busylightcommander.api.BusyLightAPI;


@Command
public class Commander implements Runnable {
    @Option(
            names = { "-v", "--volume" },
            description = "Volumne (0..7) "
    )
    private int volume = 4;

    @Option(
            names = {"-t", "--tone"},
            description = "Ringtone to play. Valid values: ${COMPLETION-CANDIDATES}"
    )
    private BusyLightAPI.Ringtone ringtone;

    @Option(
            names = {"-c", "--color"},
            description = "Color. Valid values ${COMPLETION-CANDIDATES}"
    )
    private BusyLightAPI.BLColor color;

    @Option(
            names = { "-d", "--duration" },
            description = "Duration in seconds. (default: ${DEFAULT-VALUE}) "
    )
    private int duration = 2;

    @Option(
            names = { "-b", "--blink"},
            description = "Blink. (default: ${DEFAULT-VALUE})"
    )
    private boolean blink = false;


    public void run() {
        BusyLightAPI light = new BusyLightAPI();
        boolean success = light.initDevice(
                BusyLightAPI.Vendor.PLENOM, BusyLightAPI.Product.PRODUCT_OMEGA_ID, null);
        if (! success) {
            System.exit(0);
        }

        // normalize volume (0..7)
        volume = Math.min(volume, 7);
        volume = Math.max(volume, 0);

        if (volume > 0) {
            light.setSoundEnabled(true);
            light.setVolume((short) volume);
            light.setRingTone(ringtone);
        } else {
            light.setSoundEnabled(false);
        }

        if (blink) {
            light.blinkColor(color, 5, 5);
        }
        else {
            light.steadyColor(color);
        }

        try {
            sleep(duration * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        light.stopLight();
        light.shutdown();

    }

    public static void main(String[] args) {
        CommandLine.run(new Commander(), args);
    }
}
