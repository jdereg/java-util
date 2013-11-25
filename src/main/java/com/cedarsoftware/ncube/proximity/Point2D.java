package com.cedarsoftware.ncube.proximity;


/**
 * This class is used to represent a 2D point.
 * 
 * @author John DeRegnaucourt
 */
public class Point2D implements Comparable<Point2D>, Distance<Point2D>
{
	private double x;
	private double y;
	
	public Point2D(double x, double y)
	{
		this.x = x;
		this.y = y;
	}
	
	public boolean equals(Object obj)
	{
		if (!(obj instanceof Point2D))
		{
			return false;
		}
		
		Point2D that = (Point2D) obj;
		return x == that.x && y == that.y;
	}

	public double distance(Point2D that) 
	{
		double dx = that.x - x;
		double dy = that.y - y;
		return Math.sqrt(dx * dx + dy * dy);
	}
	
	public String toString()
	{
		return "(" + x + ", " + y + ")";
	}

	public int compareTo(Point2D that)
	{
		if (x < that.x)
		{
			return -1;
		}
		if (x > that.x)
		{
			return 1;
		}
		if (y < that.y)
		{
			return -1;
		}
		if (y > that.y)
		{
			return 1;
		}
		return 0;	
	}
}
