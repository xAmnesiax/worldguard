// $Id$
/*
 * WorldGuard
 * Copyright (C) 2010 sk89q <http://www.sk89q.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.sk89q.worldguard.protection.managers;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import com.sk89q.worldedit.Vector;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.UnsupportedIntersectionException;
import com.sk89q.worldguard.protection.databases.ProtectionDatabase;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

/**
 * A very simple implementation of the region manager that uses a flat list
 * and iterates through the list to identify applicable regions. This method
 * is not very efficient.
 *
 * @author sk89q
 */
public class FlatRegionManager extends RegionManager {

    /**
     * List of protected regions.
     */
    private Map<String, ProtectedRegion> regions;

    /**
     * Construct the manager.
     *
     * @param regionloader
     */
    public FlatRegionManager(ProtectionDatabase regionloader) {
        super(regionloader);
        regions = new TreeMap<String, ProtectedRegion>();
    }

    /**
     * Get a list of protected regions.
     *
     * @return
     */
    @Override
    public Map<String, ProtectedRegion> getRegions() {
        return regions;
    }

    /**
     * Set a list of protected regions.
     */
    @Override
    public void setRegions(Map<String, ProtectedRegion> regions) {
        this.regions = new TreeMap<String, ProtectedRegion>(regions);
    }

    /**
     * Adds a region.
     *
     * @param region
     */
    @Override
    public void addRegion(ProtectedRegion region) {
        regions.put(region.getId().toLowerCase(), region);
    }

    /**
     * Removes a region and its children.
     *
     * @param id
     */
    @Override
    public void removeRegion(String id) {
        ProtectedRegion region = regions.get(id.toLowerCase());
        regions.remove(id.toLowerCase());

        if (region != null) {
            List<String> removeRegions = new ArrayList<String>();
            Iterator<ProtectedRegion> iter = regions.values().iterator();
            while (iter.hasNext()) {
                ProtectedRegion curRegion = iter.next();
                if (curRegion.getParent() == region) {
                    removeRegions.add(curRegion.getId().toLowerCase());
                }
            }

            for (String remId : removeRegions) {
                removeRegion(remId);
            }
        }
    }

    /**
     * Return whether a region exists by an ID.
     *
     * @param id
     * @return
     */
    @Override
    public boolean hasRegion(String id) {
        return regions.containsKey(id.toLowerCase());
    }

    /**
     * Get a region by its ID.
     *
     * @param id
     */
    @Override
    public ProtectedRegion getRegion(String id) {
        return regions.get(id.toLowerCase());
    }

    /**
     * Get an object for a point for rules to be applied with.
     *
     * @param pt
     * @return
     */
    @Override
    public ApplicableRegionSet getApplicableRegions(Vector pt) {
        TreeSet<ProtectedRegion> appRegions =
                new TreeSet<ProtectedRegion>();

        for (ProtectedRegion region : regions.values()) {
            if (region.contains(pt)) {
                appRegions.add(region);

                ProtectedRegion parent = region.getParent();

                while (parent != null) {
                    if (!appRegions.contains(parent)) {
                        appRegions.add(region);
                    }

                    parent = parent.getParent();
                }
            }
        }

        return new ApplicableRegionSet(appRegions, regions.get("__global__"));
    }

    /**
     * Get an object for a region for rules to be applied with.
     *
     * @return
     */
    /*@Override
    public ApplicableRegionSet getApplicableRegions(ProtectedRegion checkRegion) {

        List<ProtectedRegion> appRegions = new ArrayList<ProtectedRegion>();
        appRegions.addAll(regions.values());

        List<ProtectedRegion> intersectRegions;
        try {
            intersectRegions = checkRegion.getIntersectingRegions(appRegions);
        } catch (Exception e) {
            intersectRegions = new ArrayList<ProtectedRegion>();
        }

        return new ApplicableRegionSet(intersectRegions, regions.get("__global__"));
    }*/

    /**
     * Get a list of region IDs that contain a point.
     *
     * @param pt
     * @return
     */
    @Override
    public List<String> getApplicableRegionsIDs(Vector pt) {
        List<String> applicable = new ArrayList<String>();

        for (Map.Entry<String, ProtectedRegion> entry : regions.entrySet()) {
            if (entry.getValue().contains(pt)) {
                applicable.add(entry.getKey());
            }
        }

        return applicable;
    }

    /**
     * Get an object for a region for rules to be applied with.
     *
     * @return
     */
    @Override
    public ApplicableRegionSet getApplicableRegions(ProtectedRegion checkRegion) {

        List<ProtectedRegion> appRegions = new ArrayList<ProtectedRegion>();
        appRegions.addAll(regions.values());

        List<ProtectedRegion> intersectRegions;

        try {
            intersectRegions = checkRegion.getIntersectingRegions(appRegions);
        } catch (Exception e) {
            intersectRegions = new ArrayList<ProtectedRegion>();
        }

        return new ApplicableRegionSet(intersectRegions, regions.get("__global__"));
    }

    /**
     * Returns true if the provided region overlaps with any other region that
     * is not owned by the player.
     *
     * @param player
     * @return
     */
    @Override
    public boolean overlapsUnownedRegion(ProtectedRegion checkRegion, LocalPlayer player) {
        List<ProtectedRegion> appRegions = new ArrayList<ProtectedRegion>();

        for (ProtectedRegion other : regions.values()) {
            if (other.getOwners().contains(player)) {
                continue;
            }

            appRegions.add(other);
        }

        List<ProtectedRegion> intersectRegions;
        try {
            intersectRegions = checkRegion.getIntersectingRegions(appRegions);
        } catch (UnsupportedIntersectionException e) {
            intersectRegions = new ArrayList<ProtectedRegion>();
        }

        return intersectRegions.size() > 0;
    }

    /**
     * Get the number of regions.
     *
     * @return
     */
    @Override
    public int size() {
        return regions.size();
    }

    @Override
    public int getRegionCountOfPlayer(LocalPlayer player) {
        int count = 0;

        for (ProtectedRegion region : regions.values()) {
            if (region.getOwners().contains(player)) {
                count++;
            }
        }

        return count;
    }
}
