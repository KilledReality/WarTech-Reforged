package com.wartec.wartecmod.port.integration;

/**
 * Common IFF ownership contract for WarTech entities and tile entities.
 */
public interface ITeamOwned {
    String getOwnerTeam();

    void setOwnerTeam(String team);
}
