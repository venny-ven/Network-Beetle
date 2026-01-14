package deprecated;

import com.github.kleonaut.network_beetle.PowerObserver;
import com.github.kleonaut.network_beetle.Record;
import deprecated.regimen.Regimen;

import javax.swing.Timer;

public class Scout implements PowerObserver
{
    private final Timer timer = new Timer(2000, e -> run());
    private final Record record;
    private boolean isSwitching;

    public Scout(Record record)
    {
        this.record = record;
        timer.setRepeats(false);
    }

    public void run()
    {
        //for (String task : Tasks.fetchTasks())
        //    for (String condition : record.modeOne.conditions)
        //        if (task.equals(condition)
        //            {
        //                NetShell.setProfile(record.modeOne.netProfile);
        //                timer.setDelay(5000);
        //                timer.setActionCommand("verify");
        //                //try {
        //                //    setRegimen(regimen);
        //                //} catch (Exception e) {
        //                //    // ...
        //                //}
        //                //return;
        //            }
        //setRegimen(regimens.getLast()); // last regimen is default
    }

    public void verifyConnection()
    {

    }

    @Override
    public void setPowered(boolean flag)
    {
        if (timer.isRunning() == flag) return;
        if (timer.isRunning()) timer.stop();
        else timer.restart();
    }

    private void setRegimen(Regimen regimen)
    {
//        if(regimen != nowRegimen)
//        {
//
//        }
    }
}
