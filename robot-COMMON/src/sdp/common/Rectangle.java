package sdp.common;

import java.awt.Point;

public class Rectangle {
	
	public Point cTL;
	public Point cTR;
	public Point cBL;
	public Point cBR;
	
	public Rectangle()
	{
		cTL = new Point();
		cTR = new Point();
		cBL = new Point();
		cBR = new Point();
	}
	
	public Rectangle(Point p1, Point p2, Point p3, Point p4)
	{
		setCorners(p1, p2, p3, p4);	
	}
	
	public void setCorners(Point p1, Point p2, Point p3, Point p4)
	{
		Point ordered[] = new Point[] {p1,p2,p3,p4};
		
		
		if (ordered[0].y > ordered[1].y)
			switchPositions(ordered, 0, 1);
		
		if (ordered[2].y > ordered[3].y)
			switchPositions(ordered, 2, 3);
	
		if (ordered[1].y > ordered[3].y) //two biggest
		{
			if (ordered[0].y < ordered[2].y)
			{
				switchPositions(ordered, 1, 3);
				switchPositions(ordered, 1, 2);
			}
			else if (ordered[0].y > ordered[2].y)
			{
				if (ordered[0].y > ordered[3].y)
				{
					switchPositions(ordered, 0, 2);
					switchPositions(ordered, 1, 3);
				}
				else
				{
					switchPositions(ordered, 1, 3);
					switchPositions(ordered, 1, 2);
					switchPositions(ordered, 0, 1);
				}
			}
		}
		else if (ordered[1].y > ordered[2].y)
		{
			if (ordered[0].y > ordered[2].y)
			{
				switchPositions(ordered, 0, 2);
				switchPositions(ordered, 1, 2);
			}
			else
			{
				switchPositions(ordered, 1, 2);
			}
		}
		if (ordered[0].x > ordered[1].x)
			switchPositions(ordered, 0, 1);
		
		if (ordered[2].x > ordered[3].x)
			switchPositions(ordered, 2, 3);
		
		cTR = ordered[3];
		cTL = ordered[2];
		cBR = ordered[1];
		cBL = ordered[0];
		
	}
	
	private void switchPositions(Point array[], int pos1, int pos2)
	{
		Point temp;
		temp = array[pos1];
		array[pos1] = array[pos2];
		array[pos2] = temp;
	}
	
}
