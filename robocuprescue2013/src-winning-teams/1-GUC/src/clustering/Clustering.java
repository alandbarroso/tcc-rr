package clustering;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import java.lang.Math;

import Think.Think;

import rescuecore2.worldmodel.EntityID;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Human;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.standard.entities.StandardWorldModel;
import sample.SampleSearch;

/**
 * Clustering.java
 * 
 * 
 */
public class Clustering {
	public double[][] centroids;
	public SampleSearch search;

	/**
	 * K means
	 * 
	 * @param numberOfClusters
	 *            the number of Clusters
	 * @param model
	 *            must not be <code>null</code>
	 * @param limit
	 *            is used to avoid any infinite loops
	 * 
	 * @return an arrayList consisting of arrayLists where each one represents a
	 *         cluster and contains a list of EntityIDs of all roads in that
	 *         cluster
	 */
	public ArrayList<ArrayList<EntityID>> KMeans(int numberOfClusters,
			StandardWorldModel model, int limit,
			Collection<StandardEntity> targets, double m, double epsilon) {

		Random r = new Random();

		boolean flag = true;
		ArrayList<ArrayList<EntityID>> mGraphs = new ArrayList<ArrayList<EntityID>>();

		for (int i = 0; i < numberOfClusters; i++)
			mGraphs.add(new ArrayList<EntityID>());

		centroids = new double[numberOfClusters][2];
		// centroids[][] is the positions of the centroids in the map
		double[][] sums = new double[numberOfClusters][3];
		// sums is used to add all x's and y's of the nodes assigned to each
		// cluster and store it in the coresponding row, the third
		// entry is the number of nodes assigned to the clusters
		double width = model.getBounds().getWidth();
		// width of the map
		double height = model.getBounds().getHeight();
		// height of the map
		for (int i = 0; i < numberOfClusters; i++) {
			centroids[i][0] = r.nextDouble() * width;
			centroids[i][1] = r.nextDouble() * height;
			for (int j = 0; j < i; j++) {// assign random positions to the
				// centroids
				if (Math.hypot(Math.abs(centroids[j][0] - centroids[i][0]),
						Math.abs(centroids[j][1] - centroids[i][1])) < Math
						.hypot(width, height) / numberOfClusters) {
					i--;
					break;
				}
			}

		}
		// for(int i=0;i<numberOfClusters;i++)
		// System.out.println("x : " +centroids[i][0]+", y : "+centroids[i][1]);
		targets = model.getEntitiesOfType(StandardEntityURN.ROAD);
		// get all Roads

		int counter = 0;
		// counts the number of iterations
		while (flag && counter < limit) {
			counter++;
			for (StandardEntity object : targets) {
				// iterate over all Roads
				Area area = (Area) object;
				// object's location
				int x = area.getX();
				int y = area.getY();
				// get position of Road
				double min = Math.sqrt(Math.pow(x - centroids[0][0], 2)
						+ Math.pow(y - centroids[0][1], 2));
				// set minimum distance to be the distance between the first
				// centroid and the road
				int minIndex = 0;
				// temp variable to save the index of the closest centroid
				for (int i = 1; i < numberOfClusters; i++) {
					double dist = Math.sqrt(Math.pow(x - centroids[i][0], 2)
							+ Math.pow(y - centroids[i][1], 2));
					// calculate distance to centroids
					if (dist < min) {
						min = dist;
						minIndex = i;
					}
				}

				// update the sums array
				sums[minIndex][0] += x;
				sums[minIndex][1] += y;
				sums[minIndex][2]++;
			}

			flag = false;
			// flag to determine when to stop

			for (int i = 0; i < numberOfClusters; i++) {
				double x = sums[i][0] / sums[i][2];
				double y = sums[i][1] / sums[i][2];
				// get the new position of the centroids by dividing the sum by
				// the number of nodes assigned to the cluster
				if (x != centroids[i][0] || y != centroids[i][1])
					flag = true;
				// if there is any change in the positions of centroids set the
				// flag to true
				centroids[i][0] = x;
				centroids[i][1] = y;

				sums[i][0] = sums[i][1] = sums[i][2];
				// reset sums
			}
		}

		System.out.println("number of iterations needed: " + counter);

		for (StandardEntity object : targets) {
			Area area = (Area) object;
			// object's location
			int x = area.getX();
			int y = area.getY();
			double min = Math.sqrt(Math.pow(x - centroids[0][0], 2)
					+ Math.pow(y - centroids[0][1], 2));
			int minIndex = 0;
			for (int i = 1; i < numberOfClusters; i++) {
				double dist = Math.sqrt(Math.pow(x - centroids[i][0], 2)
						+ Math.pow(y - centroids[i][1], 2));
				if (dist < min) {
					min = dist;
					minIndex = i;
				}
			}
			mGraphs.get(minIndex).add(object.getID());
			// add the road to the cluster it's assigned to
		}

		return mGraphs;
	}

	// ///////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// kmeans++
	/**
	 * K means plus plus
	 * 
	 * @param k
	 *            number of clusters
	 * @param model
	 *            must not be <code>null</code>
	 * @param maxIterations
	 *            is used to avoid any infinite loops
	 * @param targets
	 *            a collection of all targets
	 * 
	 * @return an arrayList consisting of clusters
	 */
	public ArrayList<Cluster> KMeansPlusPlus(final int k,
			final StandardWorldModel model, final int maxIterations,
			final Collection<StandardEntity> targets) {

		// temporary array list of all targets
		ArrayList<Area> tempTargetsList = new ArrayList<Area>();

		// the agents can only deal with Buildings and Roads
		for (StandardEntity target : targets) {
			tempTargetsList.add(Think.getStandardEntityPosition(target, model));
		}
		final ArrayList<Area> targetsList = tempTargetsList;
		// array list of clusters that will be returned
		ArrayList<Cluster> clusters = chooseInitialCenters(targetsList, k,
				new Random(123), model);

		assignPointsToClusters(clusters, targetsList, model);

		// iterate through updating the centers until we're done
		final int max = (maxIterations < 0) ? Integer.MAX_VALUE : maxIterations;
		for (int count = 0; count < max; count++) {
			boolean clusteringChanged = false;
			ArrayList<Cluster> newClusters = new ArrayList<Cluster>();
			// System.out.println("iteration: "+count);
			for (final Cluster cluster : clusters) {

				// computer the new centroid for the cluster
				final double[] newCentroid = getCentroid(model, cluster.cluster);
				// System.out.println("centroid pos: "+newCentroid[0]+", "+newCentroid[1]);
				// if the centroid is not the same raise the flag
				if (newCentroid[0] != cluster.centroid[0]
						|| newCentroid[1] != cluster.centroid[1]) {
					clusteringChanged = true;
				}
				newClusters.add(new Cluster(null, newCentroid, null, 0));
			}
			if (!clusteringChanged) {
				// calculation the perc of each cluster
				// sum of all roads/buildings
				int sum = 0;

				for (int i = 0; i < clusters.size(); i++)
					sum += clusters.get(i).cluster.size();

				for (int i = 0; i < clusters.size(); i++) {
					clusters.get(i).perc = 1.0 * clusters.get(i).cluster.size()
							/ sum;
					// finally it's better to chose the center from all the
					// roads and
					// buildings in the map to be accurate
					// as much as possible
					clusters.get(i).center = (Area) getClosestEntity(
							model.getEntitiesOfType(StandardEntityURN.BUILDING,
									StandardEntityURN.ROAD),
							clusters.get(i).centroid[0],
							clusters.get(i).centroid[1]);
				}
				System.out.println("number of iterations: " + count);
				return clusters;
			}
			assignPointsToClusters(newClusters, targetsList, model);
			clusters = newClusters;
			for (int i = 0; i < clusters.size(); i++) {
				if (clusters.get(i).cluster.size() == 0)
					clusters.remove(i--);
			}
		}

		// calculation the perc of each cluster
		// sum of all roads/buildings
		int sum = 0;

		for (int i = 0; i < clusters.size(); i++)
			sum += clusters.get(i).cluster.size();

		for (int i = 0; i < clusters.size(); i++) {
			clusters.get(i).perc = 1.0 * clusters.get(i).cluster.size() / sum;
			// finally it's better to chose the center from all the roads and
			// buildings in the map to be accurate
			// as much as possible
			clusters.get(i).center = (Area) getClosestEntity(
					model.getEntitiesOfType(StandardEntityURN.BUILDING,
							StandardEntityURN.ROAD),
					clusters.get(i).centroid[0], clusters.get(i).centroid[1]);
		}
		return clusters;
	}

	private void assignPointsToClusters(final ArrayList<Cluster> clusters,
			final Collection<Area> points, StandardWorldModel model) {
		for (final Area p : points) {
			Cluster cluster = getNearestCluster(model, clusters, p);
			cluster.cluster.add(p.getID());
		}
	}

	private ArrayList<Cluster> chooseInitialCenters(
			final Collection<Area> points, final int k, Random random,
			final StandardWorldModel model) {
		ArrayList<Area> pointSet = new ArrayList<Area>(points);

		ArrayList<Cluster> resultSet = new ArrayList<Cluster>();

		// Choose one center uniformly at random from among the data points.
		final Area firstPoint = pointSet
				.remove(random.nextInt(pointSet.size()));
		final double[] center = { firstPoint.getX(), firstPoint.getY() };
		resultSet.add(new Cluster(null, center, firstPoint, 0));
		final double[] dx2 = new double[pointSet.size()];
		while (resultSet.size() < k) {
			// For each data point x, compute D(x), the distance between x and
			// the nearest center that has already been chosen.
			int sum = 0;
			for (int i = 0; i < pointSet.size(); i++) {
				final Area p = pointSet.get(i);
				final Cluster nearest = getNearestCluster(model, resultSet, p);
				final double d = model.getDistance(p, nearest.center);
				sum += d * d;
				dx2[i] = sum;
			}

			// Add one new data point as a center. Each point x is chosen with
			// probability proportional to D(x)2
			final double r = random.nextDouble() * sum;
			for (int i = 0; i < dx2.length; i++) {
				if (dx2[i] >= r) {
					final Area p = Think.getStandardEntityPosition(
							pointSet.remove(i), model);
					final double[] center2 = { p.getX(), p.getY() };
					resultSet.add(new Cluster(null, center2, p, 0));
					break;
				}
			}
		}
		return resultSet;

	}

	private Cluster getNearestCluster(StandardWorldModel model,
			ArrayList<Cluster> clusters, Area point) {
		double minDistance = Double.MAX_VALUE;
		Cluster minCluster = null;
		for (final Cluster c : clusters) {
			final double distance = Think.getEuclidianDistance(c.centroid[0],
					c.centroid[1], point.getX(), point.getY());

			// final double distance = model.getDistance(c.center, point);
			if (distance < minDistance) {
				minDistance = distance;
				minCluster = c;
			}
		}
		if (minCluster == null)
			System.out.println("NULL");
		return minCluster;
	}

	private Cluster getNearestClusterActualDistance(StandardWorldModel model,
			ArrayList<Cluster> clusters, StandardEntity point) {
		double minDistance = Double.MAX_VALUE;
		Cluster minCluster = null;
		ArrayList<EntityID> list = new ArrayList<EntityID>();
		List<EntityID> path = null;
		for (final Cluster c : clusters) {
			list.clear();
			list.add(c.center.getID());
			path = search.breadthFirstSearch(point.getID(), list);

			double distance = 0;

			for (int i = 0; i < path.size() - 2; i++)
				distance += model.getDistance(path.get(i), path.get(i + 1));

			if (distance < minDistance) {
				minDistance = distance;
				minCluster = c;
			}
		}
		if (minCluster == null)
			System.out.println("NULL");
		return minCluster;
	}

	public static double[] getCentroid(ArrayList<StandardEntity> cluster) {
		double x = 0, y = 0;
		for (StandardEntity e : cluster) {
			if (e instanceof Area) {
				Area a = (Area) e;
				x += a.getX();
				y += a.getY();
			} else {
				Human h = (Human) e;
				x += h.getX();
				y += h.getY();
			}
		}
		double[] point = { x / cluster.size(), y / cluster.size() };
		if (point[0] == Double.NaN)
			point[0] = -1000;
		if (point[1] == Double.NaN)
			point[1] = -1000;
		return point;
	}

	public static double[] getCentroidAreas(ArrayList<Area> cluster) {
		double x = 0, y = 0;
		for (Area a : cluster) {
			x += a.getX();
			y += a.getY();
		}
		double[] point = { x / cluster.size(), y / cluster.size() };
		if (point[0] == Double.NaN)
			point[0] = -1000;
		if (point[1] == Double.NaN)
			point[1] = -1000;
		return point;
	}

	public static double[] getCentroid(StandardWorldModel model,
			ArrayList<EntityID> cluster) {
		double x = 0, y = 0;
		for (EntityID e : cluster) {
			StandardEntity en = model.getEntity(e);
			if (en instanceof Area) {
				Area a = (Area) en;
				x += a.getX();
				y += a.getY();
			} else {
				Human h = (Human) en;
				x += h.getX();
				y += h.getY();
			}
		}
		if (cluster.size() == 0)
			System.out.println("cluster is empty");
		double[] point = { x / cluster.size(), y / cluster.size() };
		if (point[0] == Double.NaN)
			point[0] = -1000;
		if (point[1] == Double.NaN)
			point[1] = -1000;

		return point;
	}

	// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	/**
	 * C means
	 * 
	 * @param numberOfClusters
	 *            the number of Clusters
	 * @param model
	 *            must not be <code>null</code>
	 * @param m
	 *            fuzzification factor
	 * @param epsilon
	 *            sensitivity threshold
	 * 
	 * @return an arrayList consisting of arrayLists where each one represents a
	 *         cluster and contains a list of EntityIDs of all roads in that
	 *         cluster
	 */
	public ArrayList<ArrayList<EntityID>> CMeans(int numberOfClusters,
			Collection<StandardEntity> targets, double m, double epsilon) {

		int counter = 0;// counts the number of iterations

		ArrayList<ArrayList<EntityID>> mGraphs = new ArrayList<ArrayList<EntityID>>();
		for (int i = 0; i < numberOfClusters; i++)
			mGraphs.add(new ArrayList<EntityID>());
		// initialized array of clusters

		double[][] membership = new double[targets.size()][numberOfClusters];
		// membership of nodes in each cluster

		System.out.println("number of nodes: " + membership.length);

		for (int i = 0; i < membership.length; i++) {
			double sum = 0;
			// variable helps to normalize the memberships

			for (int j = 0; j < membership[i].length; j++) {
				membership[i][j] = Math.random() * 1000 + 1;
				sum += membership[i][j];
				// assign random memberships to the nodes for each cluster
			}

			for (int j = 0; j < membership[i].length; j++)
				membership[i][j] /= sum;
			// normalize memberships
		}

		centroids = new double[numberOfClusters][2];// position of
		// centroids

		boolean flag = true;// flag used to know when to stop iterating

		while (flag) {
			counter++;

			for (int i = 0; i < centroids.length; i++) {// iterate over all
				// centroids
				int j = 0;
				double temp0 = 0.0, temp1 = 0.0;
				centroids[i][0] = centroids[i][1] = 0.0;// reset centroids

				for (StandardEntity object : targets) {// iterate over all roads

					int x = 0, y = 0;

					if (object instanceof Area) {
						Area area = (Area) object;
						// object's location
						x = area.getX();
						y = area.getY();
					} else {
						Human human = (Human) object;
						// object's location
						x = human.getX();
						y = human.getY();
					}

					temp0 += Math.pow(membership[j][i], m);
					temp1 += Math.pow(membership[j][i], m);

					centroids[i][0] += Math.pow(membership[j][i], m) * x;
					centroids[i][1] += Math.pow(membership[j][i], m) * y;
					j++;
				}

				centroids[i][0] /= temp0;
				centroids[i][1] /= temp1;
				// compute new value of centroids
			}

			int i = 0;
			flag = false;
			for (StandardEntity object : targets) {
				// iterate over all roads
				int x = 0, y = 0;

				if (object instanceof Area) {
					Area area = (Area) object;
					// object's location
					x = area.getX();
					y = area.getY();
				} else {
					Human human = (Human) object;
					// object's location
					x = human.getX();
					y = human.getY();
				}
				// get position of road
				for (int j = 0; j < centroids.length; j++) {// iterate over all
					// centroids
					double temp = 0;
					double comm = Math.pow(
							Math.sqrt(Math.pow(x - centroids[j][0], 2)
									+ Math.pow(y - centroids[j][1], 2)),
							2 / (m - 1));
					for (int k = 0; k < centroids.length; k++) {
						// for each centroid j iterate over all centroids
						temp += Math.pow(

						1.0 / Math.sqrt(Math.pow(x - centroids[k][0], 2)
								+ Math.pow(y - centroids[k][1], 2)),
								2 / (m - 1));
					}

					temp *= comm;

					if (Math.abs(membership[i][j] - 1 / temp) >= epsilon)
						flag = true;
					// check if any difference is over the threshold

					membership[i][j] = 1 / temp;
					// compute new memberships
				}
				i++;
			}
		}

		System.out.println("number of iterations needed: " + counter);

		int i = 0;
		for (StandardEntity object : targets) {
			// iterate over all roads
			for (int j = 0; j < numberOfClusters; j++)
				// add road to each cluster where it's membership is greater or
				// equal than 1/number of clusters
				if (membership[i][j] >= 1.0 / numberOfClusters)
					mGraphs.get(j).add(object.getID());
			i++;
		}
		// removeEmptyClusters(mGraphs);
		return mGraphs;
	}

	public ArrayList<ArrayList<EntityID>> Kmeans2(int numberOfClusters,
			Collection<StandardEntity> targets, double m, double epsilon) {

		int nt = targets.size();

		// epsilon = 0.005;

		if (numberOfClusters <= 15)
			epsilon = 0.01;
		else if (numberOfClusters <= 20) {
			if (nt <= 1000)
				epsilon = 0.005;
			else if (nt <= 2000)
				epsilon = 0.008;
			else if (nt <= 3000)
				epsilon = 0.01;
			else
				epsilon = 0.02;
		} else if (numberOfClusters <= 25) {
			if (nt <= 1000)
				epsilon = 0.02;
			else if (nt <= 2000)
				epsilon = 0.03;
			else if (nt <= 3000)
				epsilon = 0.04;
			else
				epsilon = 0.05;
		} else {
			if (nt <= 1000)
				epsilon = 0.05;
			else if (nt <= 2000)
				epsilon = 0.06;
			else if (nt <= 3000)
				epsilon = 0.1;
			else
				epsilon = 0.12;
		}

		if (numberOfClusters == 0)
			return new ArrayList<ArrayList<EntityID>>();

		int counter = 0;// counts the number of iterations

		ArrayList<ArrayList<EntityID>> mGraphs = new ArrayList<ArrayList<EntityID>>();
		for (int i = 0; i < numberOfClusters; i++)
			mGraphs.add(new ArrayList<EntityID>());
		// initialized array of clusters
		Random r = new Random(123);
		double[][] membership = new double[targets.size()][numberOfClusters];
		// membership of nodes in each cluster

		System.out.println("number of nodes: " + membership.length);

		for (int i = 0; i < membership.length; i++) {
			double sum = 0;
			// variable helps to normalize the memberships

			for (int j = 0; j < membership[i].length; j++) {

				membership[i][j] = r.nextDouble() * 1000 + 1;
				sum += membership[i][j];
				// assign random memberships to the nodes for each cluster
			}

			for (int j = 0; j < membership[i].length; j++)
				membership[i][j] /= sum;
			// normalize memberships
		}

		centroids = new double[numberOfClusters][2];// position of
		// centroids

		boolean flag = true;// flag used to know when to stop iterating

		while (flag) {
			counter++;

			for (int i = 0; i < centroids.length; i++) {// iterate over all
				// centroids
				int j = 0;
				double temp0 = 0.0, temp1 = 0.0;
				centroids[i][0] = centroids[i][1] = 0.0;// reset centroids

				for (StandardEntity object : targets) {// iterate over all roads

					int x = 0, y = 0;

					if (object instanceof Area) {
						Area area = (Area) object;
						// object's location
						x = area.getX();
						y = area.getY();
					} else {
						Human human = (Human) object;
						// object's location
						x = human.getX();
						y = human.getY();
					}

					temp0 += Math.pow(membership[j][i], m);
					temp1 += Math.pow(membership[j][i], m);

					centroids[i][0] += Math.pow(membership[j][i], m) * x;
					centroids[i][1] += Math.pow(membership[j][i], m) * y;
					j++;
				}

				centroids[i][0] /= temp0;
				centroids[i][1] /= temp1;
				// compute new value of centroids
			}

			int i = 0;
			flag = false;
			for (StandardEntity object : targets) {
				// iterate over all roads
				int x = 0, y = 0;

				if (object instanceof Area) {
					Area area = (Area) object;
					// object's location
					x = area.getX();
					y = area.getY();
				} else {
					Human human = (Human) object;
					// object's location
					x = human.getX();
					y = human.getY();
				}
				// get position of road
				for (int j = 0; j < centroids.length; j++) {// iterate over all
					// centroids
					double temp = 0;
					double comm = Math.pow(
							Math.sqrt(Math.pow(x - centroids[j][0], 2)
									+ Math.pow(y - centroids[j][1], 2)),
							2 / (m - 1));
					for (int k = 0; k < centroids.length; k++) {
						// for each centroid j iterate over all centroids
						temp += Math.pow(

						1.0 / Math.sqrt(Math.pow(x - centroids[k][0], 2)
								+ Math.pow(y - centroids[k][1], 2)),
								2 / (m - 1));
					}

					temp *= comm;

					if (Math.abs(membership[i][j] - 1 / temp) >= epsilon)
						flag = true;
					// check if any difference is over the threshold

					membership[i][j] = 1 / temp;
					// compute new memberships
				}
				i++;
			}
		}

		System.out.println("number of iterations needed: " + counter);

		int i = 0, membershipIndex = 0;
		double highestMembership = 0;

		for (StandardEntity object : targets) {
			// iterate over all roads
			highestMembership = 0;
			for (int j = 0; j < numberOfClusters; j++)
				// add road to each cluster where it's membership is greater or
				// equal than 1/number of clusters
				if (membership[i][j] > highestMembership) {
					membershipIndex = j;
					highestMembership = membership[i][j];
				}

			mGraphs.get(membershipIndex).add(object.getID());
			i++;
		}
		removeEmptyClusters(mGraphs);

		return mGraphs;
	}

	public ArrayList<ArrayList<EntityID>> clustersToArrayList(
			ArrayList<Cluster> clusters) {
		ArrayList<ArrayList<EntityID>> mGraphs = new ArrayList<ArrayList<EntityID>>();
		for (Cluster c : clusters)
			mGraphs.add(c.cluster);

		return mGraphs;
	}

	/*
	 * public ArrayList<ArrayList<EntityID>> CMeansCP(int numberOfClusters,
	 * Collection<StandardEntity> targets, StandardWorldModel model, double m,
	 * double epsilon) {
	 * 
	 * GAlgorithms gra = new GAlgorithms(model);
	 * System.out.println("GAlgorithms done"); int counter = 0;// counts the
	 * number of iterations
	 * 
	 * ArrayList<ArrayList<EntityID>> mGraphs = new
	 * ArrayList<ArrayList<EntityID>>(); for (int i = 0; i < numberOfClusters;
	 * i++) mGraphs.add(new ArrayList<EntityID>()); // initialized array of
	 * clusters
	 * 
	 * ArrayList<StandardEntity> centroidsEntities = new
	 * ArrayList<StandardEntity>();
	 * 
	 * double[][] membership = new double[targets.size()][numberOfClusters]; //
	 * membership of nodes in each cluster
	 * 
	 * System.out.println("number of nodes: " + membership.length);
	 * 
	 * for (int i = 0; i < membership.length; i++) { double sum = 0; // variable
	 * helps to normalize the memberships
	 * 
	 * for (int j = 0; j < membership[i].length; j++) { membership[i][j] =
	 * Math.random() * 1000 + 1; sum += membership[i][j]; // assign random
	 * memberships to the nodes for each cluster }
	 * 
	 * for (int j = 0; j < membership[i].length; j++) membership[i][j] /= sum;
	 * // normalize memberships }
	 * 
	 * centroids = new double[numberOfClusters][2];// position of // centroids
	 * 
	 * boolean flag = true;// flag used to know when to stop iterating while
	 * (flag) { counter++;
	 * 
	 * for (int i = 0; i < centroids.length; i++) {// iterate over all //
	 * centroids int j = 0; double temp0 = 0, temp1 = 0; centroids[i][0] =
	 * centroids[i][1] = 0;// reset centroids
	 * 
	 * for (StandardEntity object : targets) {// iterate over all roads
	 * 
	 * int x = 0, y = 0;
	 * 
	 * if (object instanceof Area) { Area area = (Area) object; // object's
	 * location x = area.getX(); y = area.getY(); } else { Human human = (Human)
	 * object; // object's location x = human.getX(); y = human.getY(); }
	 * 
	 * temp0 += Math.pow(membership[j][i], m); temp1 +=
	 * Math.pow(membership[j][i], m);
	 * 
	 * centroids[i][0] += Math.pow(membership[j][i], m) * x; centroids[i][1] +=
	 * Math.pow(membership[j][i], m) * y; j++; }
	 * 
	 * centroids[i][0] /= temp0; centroids[i][1] /= temp1; // compute new value
	 * of centroids }
	 * 
	 * // reset the positions of centroids centroidsEntities = new
	 * ArrayList<StandardEntity>(); for (int i = 0; i < centroids.length; i++) {
	 * StandardEntity object = getClosestEntity(targets, centroids[i][0],
	 * centroids[i][1]); centroidsEntities.add(object); if (object instanceof
	 * Area) { Area area = (Area) object; // object's location centroids[i][0] =
	 * area.getX(); centroids[i][1] = area.getY(); } else { Human human =
	 * (Human) object; // object's location centroids[i][0] = human.getX();
	 * centroids[i][1] = human.getY(); } }
	 * 
	 * int i = 0; flag = false; for (StandardEntity object : targets) { //
	 * iterate over all targets for (int j = 0; j < centroids.length; j++) {//
	 * iterate over all // centroids double temp = 0; for (int k = 0; k <
	 * centroids.length; k++) { // for each centroid j iterate over all
	 * centroids temp += Math.pow(1.0 / gra.shortestDistance(
	 * centroidsEntities.get(k).getID(), object.getID()), 2 / (m - 1)); }
	 * 
	 * //double comm = Math.pow(gra.shortestDistance( //
	 * centroidsEntities.get(j).getID(), object.getID()), // 2 / (m - 1));
	 * double comm= Math.pow(model.getDistance(centroidsEntities.get(j).getID(),
	 * object.getID()), 2 / (m - 1)); double newMem;
	 * 
	 * if (comm == 0.0) newMem = 1.0; else newMem = 1.0 / (comm * temp);
	 * 
	 * if (Math.abs(membership[i][j] - newMem) >= epsilon) flag = true; // check
	 * if any difference is over the threshold
	 * 
	 * membership[i][j] = newMem; // compute new memberships } //
	 * System.out.println(++cc); i++; }
	 * 
	 * System.out.println(counter + "    ---------------------"); }
	 * 
	 * System.out.println("number of iterations needed: " + counter);
	 * 
	 * int i = 0; for (StandardEntity object : targets) { // iterate over all
	 * roads for (int j = 0; j < numberOfClusters; j++) // add road to each
	 * cluster where it's membership is greater or // equal than 1/number of
	 * clusters if (membership[i][j] >= 1.0 / numberOfClusters)
	 * mGraphs.get(j).add(object.getID()); i++; } return mGraphs; }
	 */
	public ArrayList<StandardEntity> rearrangeAgents(
			ArrayList<StandardEntity> agents, ArrayList<Cluster> clusters) {
		ArrayList<StandardEntity> newList = new ArrayList<StandardEntity>();
		if (agents.size() == 0)
			return newList;

		for (Cluster c : clusters) {
			StandardEntity agent = getClosestEntity(agents, c.centroid[0],
					c.centroid[1]);
			int index = 0;
			for (StandardEntity remove : agents) {
				if (agent.getID().getValue() == remove.getID().getValue())
					break;
				index++;
			}
			agents.remove(index);
			newList.add(agent);
		}
		// int i=0;
		// / for(StandardEntity ob:newList){
		// System.out.println(centroids[i][0]+", "+centroids[i][1]+" : "+((Human)ob).getX()+", "+((Human)ob).getY());
		// i++;
		// }
		return newList;
	}

	public static StandardEntity getClosestEntity(
			Collection<StandardEntity> targets, double x, double y) {

		StandardEntity result = null;
		double shortestDistance = Double.POSITIVE_INFINITY, currentDistance = 0;
		for (StandardEntity object : targets) {
			double currentX, currentY;
			if (object instanceof Area) {
				Area area = (Area) object;
				// object's location
				currentX = area.getX();
				currentY = area.getY();
			} else {
				Human human = (Human) object;
				// object's location
				currentX = human.getX();
				currentY = human.getY();
			}

			currentDistance = Math.sqrt(Math.pow(x - currentX, 2)
					+ Math.pow(y - currentY, 2));
			if (currentDistance < shortestDistance) {
				shortestDistance = currentDistance;
				result = object;
			}
		}
		return result;
	}

	public static StandardEntity getClosestEntityEntityIDs(
			StandardWorldModel model, Collection<EntityID> targets, double x,
			double y) {
		StandardEntity result = null;
		double shortestDistance = Double.POSITIVE_INFINITY, currentDistance = 0;

		for (EntityID id : targets) {
			double currentX, currentY;
			StandardEntity object = model.getEntity(id);
			if (object instanceof Area) {
				Area area = (Area) object;
				// object's location
				currentX = area.getX();
				currentY = area.getY();
			} else {
				Human human = (Human) object;
				// object's location
				currentX = human.getX();
				currentY = human.getY();
			}
			currentDistance = Math.sqrt(Math.pow(x - currentX, 2)
					+ Math.pow(y - currentY, 2));
			if (currentDistance < shortestDistance) {
				shortestDistance = currentDistance;
				result = object;
			}
		}
		return result;
	}

	public void removeEmptyClusters(ArrayList<ArrayList<EntityID>> graphs) {
		ArrayList<double[]> newCentroids = new ArrayList<double[]>();
		for (int i = 0, j = 0; i < graphs.size(); i++, j++) {
			if (graphs.get(i).size() == 0)
				graphs.remove(i--);
			else
				newCentroids.add(centroids[j]);
		}
		double[][] retCentroids = new double[newCentroids.size()][2];
		int i = 0;
		for (double[] currentCentroid : newCentroids) {
			retCentroids[i] = currentCentroid;
		}
		centroids = retCentroids;
	}

	public double[][] getCentroids() {
		return centroids;
	}

	public void printClusters(ArrayList<Cluster> clusters,
			StandardWorldModel model) {
		for (Cluster cluster : clusters) {
			System.out
					.println(cluster.center + ", x:" + cluster.centroid[0]
							/ model.getBounds().getWidth() * 100 + ", y:"
							+ cluster.centroid[1]
							/ model.getBounds().getHeight() * 100);
		}
	}

	// returns the cluster that this EntityID belongs to
	public static ArrayList<EntityID> getCluster(EntityID target,
			ArrayList<ArrayList<EntityID>> lists) {
		for (ArrayList<EntityID> list : lists)
			for (EntityID ob : list)
				if (target.getValue() == ob.getValue())
					return list;
		return null;
	}

	public static Cluster getClusterFromClusters(EntityID target,
			ArrayList<Cluster> lists) {
		for (Cluster list : lists)
			for (EntityID ob : list.cluster)
				if (target.getValue() == ob.getValue())
					return list;
		return null;
	}
	
	public static int getBuildingClusterIndex(EntityID target,
			ArrayList<Cluster> lists) {
		for (int i = 0; i < lists.size(); i++) {
			for(int j = 0; j < lists.get(i).cluster.size(); j++) {
				if(target.getValue() == lists.get(i).cluster.get(j).getValue()) {
					return i;
				}
			}
		}
		return -1;
	}

	public static Cluster getClosestClusterFromClusters(EntityID target,
			ArrayList<Cluster> lists, StandardWorldModel model) {
		double smallestDistance = Double.POSITIVE_INFINITY;
		Cluster closest = null;
		for (Cluster list : lists)
			if (model.getDistance(list.center.getID(), target) < smallestDistance) {
				closest = list;
				smallestDistance = model.getDistance(list.center.getID(),
						target);
			}
		return closest;
	}

	// returns the index of the cluster that this EntityID belongs to, -1 if it
	// does not belong to any
	public static int getClusterIndex(ArrayList<EntityID> cluster,
			ArrayList<ArrayList<EntityID>> lists) {
		for (int i = 0; i < lists.size(); i++)
			if (cluster == lists.get(i))
				return i;

		return -1;
	}

	// returns the closest Cluster to the agent
	public static ArrayList<EntityID> getClosestNotEmptyClusterIndex(
			EntityID position, ArrayList<ArrayList<EntityID>> lists,
			StandardWorldModel model) {
		int shortestDistanceSoFar = Integer.MAX_VALUE;
		int indexClosestCluster = -1;
		for (int i = 0; i < lists.size(); i++) {
			if (lists.get(i).size() == 0)
				continue;
			int tempD = model.getDistance(position, lists.get(i).get(0));
			if (tempD < shortestDistanceSoFar) {
				indexClosestCluster = i;
				shortestDistanceSoFar = tempD;
			}
		}
		if (indexClosestCluster == -1)
			return null;
		return lists.get(indexClosestCluster);
	}

	public static int getClosestNotEmptyClusterIndexFromClusters(
			EntityID position, ArrayList<Cluster> lists,
			StandardWorldModel model) {
		double shortestDistanceSoFar = Double.MAX_VALUE;
		int indexClosestCluster = -1;
		for (int i = 0; i < lists.size(); i++) {
			if (lists.get(i).cluster.size() == 0)
				continue;
			int tempD = model
					.getDistance(position, lists.get(i).center.getID());
			if (tempD < shortestDistanceSoFar) {
				indexClosestCluster = i;
				shortestDistanceSoFar = tempD;
			}
		}
		return indexClosestCluster;
	}

	public static int getClosestNotEmptyDiffClusterIndexFromClusters(
			EntityID position, ArrayList<Cluster> lists,
			StandardWorldModel model, int clusterIndex) {
		int shortestDistanceSoFar = Integer.MAX_VALUE;
		int indexClosestCluster = -1;
		for (int i = 0; i < lists.size(); i++) {
			if (lists.get(i).cluster.size() == 0)
				continue;
			int tempD = model
					.getDistance(position, lists.get(i).center.getID());
			if (tempD < shortestDistanceSoFar && i != clusterIndex) {
				indexClosestCluster = i;
				shortestDistanceSoFar = tempD;
			}
		}
		return indexClosestCluster;
	}
	
	public static int getClosestNotEmptyDiffClusterPolice(
			EntityID position, ArrayList<Cluster> lists,
			StandardWorldModel model, int clusterIndex) {
		int shortestDistanceSoFar = Integer.MAX_VALUE;
		int indexClosestCluster = -1;
		for (int i = 0; i < lists.size(); i++) {
			if (Think.getClusterBuildingsEntrance(model, lists.get(i)).isEmpty())
				continue;
			int tempD = model
					.getDistance(position, lists.get(i).center.getID());
			if (tempD < shortestDistanceSoFar && i != clusterIndex) {
				indexClosestCluster = i;
				shortestDistanceSoFar = tempD;
			}
		}
		return indexClosestCluster;
	}

}
