package com.wartec.wartecmod.compat;

/** Persistent command-network identity used by radar and weapon IFF. */
public interface ITeamOwned {
    String getOwnerTeam();
    void setOwnerTeam(String team);
}
