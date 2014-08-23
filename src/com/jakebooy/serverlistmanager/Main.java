package com.jakebooy.serverlistmanager;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.server.ServerListPingEvent;
import org.bukkit.plugin.java.JavaPlugin;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerOptions;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import com.comphenix.protocol.wrappers.WrappedServerPing;

public class Main extends JavaPlugin implements Listener{
	private FileConfiguration customConfig = null;
	private File ipConfig = null;
	ArrayList<WrappedGameProfile> players = new ArrayList<WrappedGameProfile>();
	public void onEnable(){
		Bukkit.getServer().getPluginManager().registerEvents(this, this);
    	ProtocolLibrary.getProtocolManager().addPacketListener(
    			new PacketAdapter(this, ListenerPriority.NORMAL,
    			Arrays.asList(PacketType.Status.Server.OUT_SERVER_INFO), ListenerOptions.ASYNC) {
    			 
    			@Override
    			public void onPacketSending(PacketEvent event) {
    			handlePing(event.getPacket().getServerPings().read(0));
    			}
    			});
		this.getServer().getPluginManager().registerEvents(this, this);
	    getLogger().info("ServerListManager by Jakebooy has been enabled"); 
	    saveDefaultConfig();
	    saveIp();
	}
	public void onDisable(){
		getLogger().info("ServerListManager by Jakebooy has been disabled.");
	}
	@SuppressWarnings("deprecation")
	public void reloadIp() {
	    if (ipConfig == null) {
	    ipConfig = new File(getDataFolder(), "ips.yml");
	    }
	    customConfig = YamlConfiguration.loadConfiguration(ipConfig);
	 
	    InputStream defConfigStream = this.getResource("ips.yml");
	    if (defConfigStream != null) {
	        YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(defConfigStream);
	        customConfig.setDefaults(defConfig);
	    }
	}
	public boolean onCommand(CommandSender sender, Command command,
			String commandLabel, String[] args) {		
		if (command.getLabel().equalsIgnoreCase("serverpingreload")){
			if(sender.isOp() == false){
				sender.sendMessage("You must be an OP!");
			}else{
			reloadIp();
			reloadConfig();
			sender.sendMessage(ChatColor.GOLD + "Configs reloaded!");
			}
		}
		return false;
	}
	public void saveIp() {
	    if (customConfig == null || ipConfig == null) {
	        return;
	    }
	    try {
	        getIp().save(ipConfig);
	    } catch (IOException ex) {
	        getLogger().log(Level.SEVERE, "Could not save config to " + ipConfig, ex);
	    }
	}
	public FileConfiguration getIp() {
	    if (customConfig == null) {
	        reloadIp();
	    }
	    return customConfig;
	}
	private void handlePing(WrappedServerPing ping){
		if(getConfig().getBoolean("disable_player_slots") == false){
		if(getConfig().getBoolean("enable_fake_players") == (true)){
			ping.setPlayersMaximum(getConfig().getInt("faked_max_players"));
			ping.setPlayersOnline(getConfig().getInt("faked_online_players"));
		}
		}else{
			ping.setPlayersVisible(false);
		}
		if(getConfig().getBoolean("enable_fake_version") == true){
			String ver = getConfig().getString("fake_version_text");
	        ping.setVersionProtocol(-1);
	        String version = ver.replace('&', '§').replaceAll("%max%", Integer.toString(ping.getPlayersMaximum())).replaceAll("%online%", Integer.toString(ping.getPlayersOnline()));
			ping.setVersionName(version);			
		}
		
		if(getConfig().getBoolean("enable_custom_playerlist_popover") == true){
		players.clear();
		List<String> lines = getConfig().getStringList("playerlist_popover_text");
		for(int i = 0; i < lines.size(); i++){
			
			players.add(new WrappedGameProfile(Integer.toString(i), lines.get(i).replace('&', '§').replaceAll("%max%", Integer.toString(ping.getPlayersMaximum())).replaceAll("%online%", Integer.toString(ping.getPlayersOnline()))));
			ping.setPlayers(players);
			
		}
		}
    	}
	@EventHandler
	public void onRandomMOTDJoin(PlayerJoinEvent e){
		Player p = e.getPlayer();
		String ip = p.getAddress().toString().replace("/", "").split(":")[0].replace(".", "_");
		getIp().set(ip + ".name", p.getName());
		saveIp();
	}
	@EventHandler
	public void onPing(ServerListPingEvent e){
	    List<String> motds = getConfig().getStringList("server_list_motds");
		Random rand = new Random();
		int Low = 0;
		int High = motds.size();
		int R = rand.nextInt(High-Low) + Low;
	    String motd = motds.get(R).replace('&', '§');
		String ip = e.getAddress().toString().replace("/", "").split(":")[0].replace(".", "_");
	    if(getIp().contains(ip)){
	    	String name = getIp().getString(ip + ".name");
	    		String finalMotd = motd.replace("%name%", name).replace("$n", "\n").replaceAll("%max%", Integer.toString(Bukkit.getServer().getMaxPlayers())).replaceAll("%online%", Integer.toString(Bukkit.getServer().getOnlinePlayers().size()));
	    	    e.setMotd(finalMotd);
	    	}else{
	    		String defaultMOTDl = getConfig().getString("default_motd_for_new_player");
	    	    String defaultMOTD = defaultMOTDl.replace('&', '§').replace("$n", "\n").replaceAll("%max%", Integer.toString(Bukkit.getServer().getMaxPlayers())).replaceAll("%online%", Integer.toString(Bukkit.getServer().getOnlinePlayers().size()));
	    		e.setMotd(defaultMOTD);
	    	}
	}
}