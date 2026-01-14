package deprecated;

import com.github.kleonaut.network_beetle.Main;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

public class AutorunWriter
{
    public static void setLaunchedOnStartup(boolean flag)
    {
        if (flag)
        {
            try {
                File jar = new File(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
                String path = jar.getAbsolutePath();
                Runtime.getRuntime().exec("reg add HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Run /v NetworkBeetle /d \"\\\"" + path + "\\\"\" /f");
            } catch (IOException | URISyntaxException e) {
                throw new RuntimeException(e);
            }
        }
        else
        {
            try {
                Runtime.getRuntime().exec("reg delete HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Run /v NetworkBeetle /f");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
