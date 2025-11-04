package net.frealac.iamod.ai.pathfinding;

import net.minecraft.core.BlockPos;

/**
 * Represents a node in the pathfinding graph.
 */
public class PathNode implements Comparable<PathNode> {
    public final BlockPos pos;
    public PathNode parent;
    public double gCost; // Cost from start to this node
    public double hCost; // Heuristic cost from this node to goal
    public double fCost; // Total cost (g + h)

    public PathNode(BlockPos pos) {
        this.pos = pos;
        this.gCost = 0;
        this.hCost = 0;
        this.fCost = 0;
    }

    public void calculateCosts(BlockPos goal, double parentGCost) {
        this.gCost = parentGCost + 1.0;
        this.hCost = calculateHeuristic(goal);
        this.fCost = this.gCost + this.hCost;
    }

    private double calculateHeuristic(BlockPos goal) {
        // Manhattan distance
        int dx = Math.abs(pos.getX() - goal.getX());
        int dy = Math.abs(pos.getY() - goal.getY());
        int dz = Math.abs(pos.getZ() - goal.getZ());
        return dx + dy + dz;
    }

    @Override
    public int compareTo(PathNode other) {
        return Double.compare(this.fCost, other.fCost);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof PathNode other)) return false;
        return pos.equals(other.pos);
    }

    @Override
    public int hashCode() {
        return pos.hashCode();
    }
}
