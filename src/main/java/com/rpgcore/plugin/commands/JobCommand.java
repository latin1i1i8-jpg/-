package com.rpgcore.plugin.commands;

import com.rpgcore.plugin.RpgCorePlugin;
import com.rpgcore.plugin.data.PlayerData;
import com.rpgcore.plugin.job.JobType;
import com.rpgcore.plugin.util.Msg;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * /직업 (=/job)        -> 직업 목록 표시
 * /전직 <이름|번호>     -> 직업 선택/변경 (첫 전직 무료, 이후 변경 30,000G)
 *
 * ⭐ 베드락 배려: 휴대폰/콘솔에서 한글 입력이 불편하므로
 *   - 영문 별칭(/job, /setjob) 지원
 *   - 이름 대신 번호로도 선택 가능 (/setjob 1)
 */
public class JobCommand implements CommandExecutor {

    private static final long JOB_CHANGE_COST = 30_000L;

    private final RpgCorePlugin plugin;

    public JobCommand(RpgCorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("플레이어만 사용할 수 있는 명령어입니다.");
            return true;
        }

        // 별칭(/job 등)으로 실행돼도 항상 등록된 이름 기준으로 판단
        if (command.getName().equals("직업")) {
            listJobs(player);
            return true;
        }

        // /전직
        if (args.length < 1) {
            Msg.send(player, ChatColor.RED + "사용법: /전직 <직업이름 또는 번호>  (예: /전직 전사, /전직 1)");
            listJobs(player);
            return true;
        }

        List<JobType> ordered = new ArrayList<>(List.of(JobType.values()));
        JobType job = null;

        // 번호 선택 (베드락에서 한글 입력 없이 고르기)
        try {
            int index = Integer.parseInt(args[0].trim());
            if (index >= 1 && index <= ordered.size()) {
                job = ordered.get(index - 1);
            }
        } catch (NumberFormatException ignored) {
            // 숫자가 아니면 이름으로 찾기
        }

        if (job == null) {
            String name = String.join(" ", args).trim();
            job = JobType.fromDisplayName(name);
            if (job == null) {
                // 영문 이름도 허용 (warrior/mage/archer/rogue)
                for (JobType type : ordered) {
                    if (type.name().equalsIgnoreCase(name)) {
                        job = type;
                        break;
                    }
                }
            }
        }

        if (job == null) {
            Msg.send(player, ChatColor.RED + "존재하지 않는 직업입니다. /직업 으로 목록을 확인하세요.");
            return true;
        }

        PlayerData data = plugin.getDataManager().get(player.getUniqueId());
        if (job.getDisplayName().equals(data.getJob())) {
            Msg.send(player, ChatColor.YELLOW + "이미 " + job.getDisplayName() + " 입니다.");
            return true;
        }

        boolean first = data.getJob() == null;
        if (!first) {
            if (data.getGold() < JOB_CHANGE_COST) {
                Msg.send(player, ChatColor.RED + "직업 변경 비용 " + JOB_CHANGE_COST + "G 가 필요합니다. (보유: " + data.getGold() + "G)");
                return true;
            }
            data.addGold(-JOB_CHANGE_COST);
        }
        data.setJob(job.getDisplayName());
        plugin.getDataManager().save(data);
        com.rpgcore.plugin.job.PlayerStatApplier.apply(player, data);

        String costTxt = first ? "첫 전직 무료!" : ("비용 " + JOB_CHANGE_COST + "G");
        Msg.send(player, ChatColor.GREEN + "✨ " + job.getDisplayName() + " (으)로 전직했습니다! (" + costTxt + ")");
        Msg.send(player, ChatColor.GRAY + job.getDescription());
        return true;
    }

    private void listJobs(Player player) {
        PlayerData data = plugin.getDataManager().get(player.getUniqueId());
        Msg.send(player, ChatColor.GOLD + "===== 직업 목록 =====");
        int index = 1;
        for (JobType type : JobType.values()) {
            String mark = type.getDisplayName().equals(data.getJob()) ? ChatColor.AQUA + " (현재 직업)" : "";
            Msg.send(player, ChatColor.WHITE + "" + index + ". " + type.getDisplayName()
                    + ChatColor.GRAY + " : " + type.getDescription() + mark);
            index++;
        }
        Msg.send(player, ChatColor.GRAY + "/전직 <이름 또는 번호> 로 선택 (첫 전직 무료, 이후 변경 " + JOB_CHANGE_COST + "G)");
        Msg.send(player, ChatColor.DARK_GRAY + "베드락: /setjob 1 처럼 번호로 입력하면 편합니다.");
    }
}
