package emu.grasscutter.task.tasks;

import emu.grasscutter.Grasscutter;
import emu.grasscutter.game.managers.AnnouncementManager;
import emu.grasscutter.task.Task;
import emu.grasscutter.task.TaskHandler;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Task(taskName = "Announcement", taskCronExpression = "0 * * * * ?", triggerName = "AnnouncementTrigger")
public final class AnnouncementTask extends TaskHandler {

    Map<Integer, Integer> intervalMap = new ConcurrentHashMap<>();
    @Override
    public void onEnable() {
        Grasscutter.getLogger().debug("[Task] Announcement task enabled.");
    }

    @Override
    public void onDisable() {
        Grasscutter.getLogger().debug("[Task] Announcement task disabled.");
    }

    @Override
    public synchronized void execute(JobExecutionContext context) throws JobExecutionException {
        var current = new Date();
        var announceConfigItems = Grasscutter.getGameServer().getAnnouncementManager().getAnnounceConfigItemMap().values().stream()
            .filter(AnnouncementManager.AnnounceConfigItem::isTick)
            .filter(i -> current.after(i.getBeginTime()))
            .filter(i -> current.before(i.getEndTime()))
            .collect(Collectors.toMap(AnnouncementManager.AnnounceConfigItem::getTemplateId, y -> y));

        announceConfigItems.values().forEach(i -> intervalMap.compute(i.getTemplateId(), (k,v) -> v == null ? 1 : v + 1));

        var toSend = intervalMap.entrySet().stream()
            .filter(i -> announceConfigItems.containsKey(i.getKey()))
            .filter(i -> announceConfigItems.get(i.getKey()).getInterval() >= i.getValue())
            .map(i -> announceConfigItems.get(i.getKey()))
            .toList();

        Grasscutter.getGameServer().getAnnouncementManager().broadcast(toSend);
        Grasscutter.getLogger().debug("Broadcast {} announcement(s) to all online players", toSend.size());

        // clear the interval count
        toSend.forEach(i -> intervalMap.put(i.getTemplateId(), 0));
    }
}
