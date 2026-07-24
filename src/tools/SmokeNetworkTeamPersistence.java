package tools;

import com.wartec.wartecmod.compat.NetworkTeamHelper;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

public final class SmokeNetworkTeamPersistence {
    public static void main(String[] args) {
        NBTTagCompound saved = new NBTTagCompound();
        FakeWorld firstWorld = new FakeWorld();
        FakePlayer first = new FakePlayer(firstWorld, "TestPilot", saved);

        assertEquals("alpha", NetworkTeamHelper.setPlayerTeam(first, "alpha"));
        assertEquals("alpha", firstWorld.scoreboard.playerTeam);
        assertEquals("alpha", NetworkTeamHelper.getPlayerTeam(first));

        FakeWorld reconnectWorld = new FakeWorld();
        FakePlayer reconnect = new FakePlayer(
                reconnectWorld, "TestPilot", saved);
        assertEquals("alpha", NetworkTeamHelper.getPlayerTeam(reconnect));
        assertEquals("alpha", reconnectWorld.scoreboard.playerTeam);

        reconnectWorld.scoreboard.func_96527_f("bravo");
        reconnectWorld.scoreboard.playerTeam = "bravo";
        assertEquals("bravo", NetworkTeamHelper.getPlayerTeam(reconnect));

        FakeWorld secondReconnectWorld = new FakeWorld();
        FakePlayer secondReconnect = new FakePlayer(
                secondReconnectWorld, "TestPilot", saved);
        assertEquals("bravo", NetworkTeamHelper.getPlayerTeam(secondReconnect));

        assertEquals("player:TestPilot",
                NetworkTeamHelper.setPlayerTeam(secondReconnect, "personal"));
        assertEquals("", secondReconnectWorld.scoreboard.playerTeam);
        assertEquals("player:TestPilot",
                NetworkTeamHelper.getPlayerTeam(secondReconnect));
        if (!NetworkTeamHelper.areFriendly("alpha", "alpha")
                || NetworkTeamHelper.areFriendly("alpha", "bravo")) {
            throw new AssertionError("IFF comparison failed");
        }
        System.out.println("TEAM_PERSISTENCE_PASS");
    }

    private static void assertEquals(String expected, String actual) {
        if (!expected.equals(actual)) {
            throw new AssertionError("Expected " + expected + " but got " + actual);
        }
    }

    public static final class FakePlayer extends EntityPlayer {
        private final String name;
        private final NBTTagCompound data;

        FakePlayer(World world, String name, NBTTagCompound data) {
            super(world);
            this.name = name;
            this.data = data;
        }

        public String func_70005_c_() {
            return name;
        }

        public FakeTeam func_96124_cp() {
            FakeScoreboard scoreboard = ((FakeWorld) field_70170_p).scoreboard;
            return scoreboard.playerTeam.length() == 0
                    ? null : scoreboard.teams.get(scoreboard.playerTeam);
        }

        @Override
        public NBTTagCompound getEntityData() {
            return data;
        }
    }

    public static final class FakeWorld extends World {
        final FakeScoreboard scoreboard = new FakeScoreboard();

        public FakeScoreboard func_96441_U() {
            return scoreboard;
        }
    }

    public static final class FakeScoreboard {
        final Map<String, FakeTeam> teams = new HashMap<String, FakeTeam>();
        String playerTeam = "";

        public FakeTeam func_96509_i(String name) {
            return teams.get(name);
        }

        public FakeTeam func_96527_f(String name) {
            FakeTeam team = new FakeTeam(name);
            teams.put(name, team);
            return team;
        }

        public boolean func_151392_a(String player, String team) {
            if (!teams.containsKey(team)) {
                return false;
            }
            playerTeam = team;
            return true;
        }

        public boolean func_96524_g(String player) {
            playerTeam = "";
            return true;
        }
    }

    public static final class FakeTeam {
        private final String name;

        FakeTeam(String name) {
            this.name = name;
        }

        public String func_96661_b() {
            return name;
        }
    }
}
