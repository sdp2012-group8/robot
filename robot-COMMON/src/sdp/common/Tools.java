package sdp.common;

import java.awt.Point;

public class Tools {
	
	public static boolean doesLineIntersectRect(Line line, Rectangle rect, boolean ignoreEndPoints)
	{
		boolean cornersAboveLine[] = new boolean[] {	line.isPointAboveLine(rect.cTL),
														line.isPointAboveLine(rect.cTR),
														line.isPointAboveLine(rect.cBL),
														line.isPointAboveLine(rect.cBR)
													};
		
		if ((cornersAboveLine[0] == cornersAboveLine[1]) && (cornersAboveLine[1] == cornersAboveLine[2]) && (cornersAboveLine[2] == cornersAboveLine[3]))
    	{
//			System.out.println("all Points the same side of line");
    		return false;
    	}
    	else
    	{
//    		System.out.println("Different sides of line");
    		int leftMost = rect.cTL.x < rect.cBL.x ?rect.cTL.x : rect.cBL.x;
    		int rightMost = rect.cTR.x > rect.cBR.x?rect.cTR.x:rect.cBR.x;
    		int topMost = rect.cTL.y > rect.cTR.y?rect.cTL.y:rect.cTR.y;
    		int bottomMost = rect.cBL.y < rect.cBR.y?rect.cBL.y:rect.cBR.y;
//    		
//    		System.out.println("LEft : " + leftMost);
//    		System.out.println("Right : " + rightMost);
//    		System.out.println("Top : " + topMost);
//    		System.out.println("Bottom : " + bottomMost);
    		
    		return  ignoreEndPoints ||
    			   !((line.p1.x < leftMost && line.p2.x < leftMost) ||
    				(line.p1.x > rightMost && line.p2.x > rightMost) ||	/*line starts after right of rectangle */
    				(line.p1.y > topMost && line.p2.y > topMost) ||			/*line stops before top of rectangle */
    				(line.p1.y < bottomMost && line.p2.y < bottomMost));		/*line starts after bottom of rectangle */
    	}
    	
	}


	public static double getDistBetweenPoints(Point p1, Point p2)
    {
    	return Math.sqrt((double) (Math.pow(p1.x-p2.x,2) + (Math.pow(p1.y-p2.y,2))));
    }
    
    public static Point getRelativePos(Point startPoint, Point relativePoint)
    {
    	return new Point(relativePoint.x - startPoint.x,relativePoint.y-startPoint.y);
    }
    
    
    public static double getAngleFrom0_0(Point pos)
    {
    	//deals with cases where pos is on the x-axis
    	if (pos.y == 0)
    	{
    		return (pos.x > 0 ? 0: Math.PI);
    	}
    	else
    	{
    		if (pos.x > 0)
    			return (Math.atan(((float) pos.y) / pos.x));
    		else
    			return (Math.PI + Math.atan(((float) pos.y) / pos.x));
    	}
    }
    
    public static void printCoors(String s, Point c)
    {
    	System.out.println(s + c.x + ", " + c.y);
    }
    
    public static void rest(int howLong)
    {
    	try {
			Thread.sleep(howLong);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
    }

    public static double getAngleToFacePoint(Point ourCoor, double angle, Point target) {

		// first I want to find where the target is in relation to our robot
		Point targetRelativePos = Tools.getRelativePos(ourCoor, target);
		
		double targetFromNxt = Tools.getAngleFrom0_0(targetRelativePos);

		
		if (targetFromNxt < 0)
			targetFromNxt = 2 * Math.PI + targetFromNxt;

		// now find how much our robot has to turn to face target
		// (turning by negative getAngle returns it to face 0 then add on ball Angle
		double howMuchToTurn = angle - targetFromNxt;

		// now adjust it so that it turns in the shortest direction (clockwise or counter clockwise)
		if (howMuchToTurn < -Math.PI)
			howMuchToTurn  = 2*Math.PI + howMuchToTurn;
		else if (howMuchToTurn > Math.PI)
			howMuchToTurn = - (2 * Math.PI - howMuchToTurn);

		
		return howMuchToTurn;

	}
	

	/**
	 * Calculates distance between two points.
	 * @param p1 start point
	 * @param p2 end point
	 * @return sqrt((x1-x2)^2+(y1-y2)^2)
	 */
	public static double getDistanceBetweenPoint(Point p1, Point p2)
	{
		return Math.sqrt((double)(Math.pow(p1.x-p2.x,2)+(Math.pow(p1.y-p2.y,2))));
	}
    /**
     * Rotate point p2 around point p1 by deg degrees
     * @param p1 rotation point
     * @param p2 point to be rotated
     * @param deg degrees
     * @return new rotated coordinates
     */
    public static Point rotatePoint(Point p1, Point p2, int deg)
    {
    	p2.x -= p1.x;
    	p2.y -= p1.y;
    	double rad = (double) Math.toRadians(deg);
    	int xtemp;
    	xtemp = (int) Math.round((p2.x * (double)Math.cos(rad)) - (p2.y * (double)Math.sin(rad)));
    	p2.y = (int) Math.round((p2.x * (double)Math.sin(rad)) + (p2.y * (double)Math.cos(rad)));
    	p2.x = xtemp;
    	return new Point (p2.x+p1.x, p2.y+p1.y);
    }
    
    /**
     * Find the most common value in the given array
     * @param in the input array
     * @return the most common value
     */
    public static int goodAvg(int[] in){
    	int[] values = new int[in.length];
    	int[] counts = new int[in.length];
    	for(int i = 0; i < in.length; i++){
    		int c = gotInt(values, in[i]);
    		if(c != -1){
    			counts[c]++;
    		} else {
    	    	for(int ii = 0; ii < in.length; ii++){
    	    		if(values[ii] == 0)
    	    			values[ii] = in[ii];
    	    	}
    		}
    	}
    	int max = 0;
    	int maxi = -1;
    	for(int i = 0; i < in.length; i++){
    		if(counts[i] > max){
    			max = counts[i];
    			maxi = i;
    		}
    	}
    	return values[maxi];
    }
    /**
     * Check if array in contains integer n
     * @param in input array of ints
     * @param n search for this number
     * @return position or -1 if not found
     */
    public static int gotInt(int[] in, int n){
    	for(int i = 0; i < in.length; i++){
    		if(in[i] == n)
    			return i;
    	}
    	return -1;
    }
    /**
     * Push int n into array in. 0th element will be the new element.
     * last element will be lost
     * @param in array to be appended
     * @param n int to be inserted
     * @return new array
     */
    public static int[] push(int[] in, int n) {
		for(int i = in.length-1; i > 0; i--){
			in[i] = in[i-1];
		}
		in[0] = n;
		return in;
    }

}
