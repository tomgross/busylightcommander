package com.itconsense.busylightcommander.api;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.junit.Test;

/**
 * Unit test for simple App.
 */
public class BusyLightAPITest
{

    @Test
    public void convertHexToPWM()
    {
        short[] color = BusyLightAPI.convertHexToPWM(255, 0, 0);
        short[] expected = {100, 0, 0};
        assertArrayEquals(color, expected);
    }

    @Test
    public void convertHexToPWM_illegal()
    {
        short[] expected = {0, 0, 0};
        final ByteArrayOutputStream myStdErr = new ByteArrayOutputStream();
        System.setErr(new PrintStream(myStdErr));

        short[] color = BusyLightAPI.convertHexToPWM(256, 0, 0);

        final String stderr = myStdErr.toString();
        assertArrayEquals(color, expected);
        assertTrue(stderr.contains("invalid RGB value(s): 256 , 0 , 0"));
    }

  @Test
  public void main() {}

  @Test
  public void _steadyColor() {}

  @Test
  public void steadyColor() {}

  @Test
  public void steadyColor1() {}

  @Test
  public void keepAlive() {}

  @Test
  public void stopLight() {}

  @Test
  public void _blinkColor() {}

  @Test
  public void blinkColor() {}

  @Test
  public void blinkColor1() {}

  @Test
  public void initHidServices() {}

  @Test
  public void initDevice() {}

  @Test
  public void ping() {}

  @Test
  public void rainbow() {}

  @Test
  public void shutdown() {}

  @Test
  public void hidDeviceAttached() {}

  @Test
  public void hidDeviceDetached() {}

  @Test
  public void hidFailure() {}

  @Test
  public void isSoundEnabled() {}

  @Test
  public void setSoundEnabled() {}

  @Test
  public void getRingTone() {}

  @Test
  public void setRingTone() {}

  @Test
  public void getVolume() {}

  @Test
  public void setVolume() {}

  @Test
  public void sendBytes() {}
}
