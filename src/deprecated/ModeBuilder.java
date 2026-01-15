package deprecated;

import com.github.kleonaut.network_beetle.Mode;
import com.github.kleonaut.network_beetle.NetProfile;

import java.util.ArrayList;
import java.util.List;

public class ModeBuilder
{
    private String name = "New Mode";
    private NetProfile netProfile = NetProfile.STAY;
    private List<String> conditions = new ArrayList<>();

    public ModeBuilder setName(String name) { this.name = name; return this; }
    public ModeBuilder setProfile(NetProfile profile) { this.netProfile = profile; return this; }
    public ModeBuilder setConditions(List<String> conditions) { this.conditions = conditions; return this; }

    public Mode get() { return new Mode(name, netProfile, conditions); }
}
