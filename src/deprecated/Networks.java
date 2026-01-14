package deprecated;

import com.github.kleonaut.network_beetle.MainWindow;
import com.github.kleonaut.network_beetle.NetProfile;
import com.github.kleonaut.network_beetle.Regex;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Networks
{
    public static void setProfile(NetProfile profile)
    {
        if (profile == NetProfile.STAY)
        {
            MainWindow.addToLog("Remaining on the same network");
            return;
        }
        try {
            if (profile == NetProfile.DISCONNECT)
            {
                Runtime.getRuntime().exec("netsh wlan disconnect");
                MainWindow.addToLog("Disconnecting from the Internet");
            }
            else
            {
                Runtime.getRuntime().exec("netsh wlan connect name=\"" + profile.name() + "\"");
                MainWindow.addToLog("Connecting to "+profile.name());
            }
        } catch (IOException e) { throw new RuntimeException(e); }
    }

    // TODO: make this work with hidden networks too
    public static List<String> fetchNearbyNetworks()
    {
        return runAndParse("netsh wlan show networks", Regex.SSID);
    }

    public static List<NetProfile> fetchAllProfiles()
    {
        List<String> results = runAndParse("netsh wlan show profiles", Regex.ALL_PROFILES);
        List<NetProfile> profiles = new ArrayList<>();
        for (String item : results)
            profiles.add(NetProfile.get(item));
        return profiles;
    }

    public static NetProfile fetchNowProfile()
    {
        List<String> results = runAndParse("netsh wlan show interfaces", Regex.PROFILE);
        if (results.isEmpty()) return NetProfile.DISCONNECT;
        else return NetProfile.get(results.getFirst());
    }

    public static List<NetProfile> fetchNearbyProfiles()
    {
        List<NetProfile> nearbyProfiles = new ArrayList<>();

        List<String> networks = fetchNearbyNetworks();
        List<NetProfile> profiles = fetchAllProfiles();
        for (String network : networks)
            for (NetProfile profile : profiles)
                if (profile.name().equals(network))
                    nearbyProfiles.add(profile);

        return nearbyProfiles;
    }

    private static List<String> runAndParse(String command, Regex rgx)
    {
        List<String> results = new ArrayList<>();
        try {
            Process netshProcess = Runtime.getRuntime().exec(command);
            try (
                    InputStream byteIn = netshProcess.getInputStream();
                    Scanner scanner = new Scanner(byteIn).useDelimiter(rgx.NEWLINE.get());
            ) {
                // search for something like "Profile : " pattern with no limit (horizon=0)
                while (scanner.findWithinHorizon(rgx.get(), 0) != null)
                    results.add(scanner.next().trim());
            }
        } catch (IOException e) { throw new RuntimeException(e); }
        return List.copyOf(results);
    }
}
