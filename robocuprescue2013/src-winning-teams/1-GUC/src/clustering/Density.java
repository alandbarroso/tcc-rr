package clustering;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import Think.Think;

import rescuecore2.standard.entities.Human;

import rescuecore2.standard.entities.StandardWorldModel;
import rescuecore2.worldmodel.EntityID;

public class Density {
	public void distributeClusters(ArrayList<ArrayList<EntityID>> mGraphs,
			ArrayList<EntityID> agents, String fileName, boolean append) {

		if (agents.size() == 0)
			return;
		String plan = "";
		double[] clusterDensity = new double[mGraphs.size()];
		int numberOfRoads = 0, i = 0;
		for (ArrayList<EntityID> listx : mGraphs) {
			clusterDensity[i] = listx.size();
			numberOfRoads += clusterDensity[i++];
		}

		System.out.print("Densities: ");

		for (i = 0; i < mGraphs.size(); i++) {
			clusterDensity[i] /= numberOfRoads;
			clusterDensity[i] *= agents.size();
		}

		i = 0;
		int j = 0;
		for (ArrayList<EntityID> listx : mGraphs) {
			System.out.print(Math.round(clusterDensity[j]) + "=="
					+ clusterDensity[j] + " ");
			for (int count = 0; count < Math.round(clusterDensity[j])
					&& i < agents.size(); count++) {
				plan += agents.get(i++).toString() + "\n";
				for (EntityID node : listx)
					plan += (node.toString() + " ");
				plan += "\n";
			}
			j++;
		}
		System.out.println("\n" + i);
		for (; i < agents.size(); i++) {
			ArrayList<EntityID> listx = mGraphs.get(i % mGraphs.size());
			plan += agents.get(i).toString() + "\n";
			for (EntityID node : listx)
				plan += (node.toString() + " ");
			plan += "\n";
		}
		System.out.println(i);
		System.out.println();

		try {
			FileWriter fw = new FileWriter(fileName, append);
			if (append)
				fw.write(plan);
			else
				fw.append(plan);
			fw.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void distributeClusters(StandardWorldModel model,
			ArrayList<ArrayList<EntityID>> mGraphs, double[][] centroids,
			ArrayList<EntityID> agents, String fileName) {

		// sum of all roads/buildings
		int sum = 0;

		for (int i = 0; i < mGraphs.size(); i++)
			sum += mGraphs.get(i).size();

		// getting perc for all clusters
		Cluster[] clusters = new Cluster[mGraphs.size()];

		for (int i = 0; i < mGraphs.size(); i++) {
			double perc = 1.0 * mGraphs.get(i).size() / sum;
			clusters[i] = new Cluster(mGraphs.get(i), centroids[i], null, perc);
		}

		// sorting clusters
		Arrays.sort(clusters);

		// loop on clusters, assign agents to each
		for (int i = 0; i < clusters.length - 1; i++) {

			// number of agents for cluster
			int num = (int) (clusters[i].perc * agents.size());
			if (num == 0)
				num = 1;

			// assign closest agents
			for (int j = 0; j < num; j++) {
				double min = Double.POSITIVE_INFINITY;
				int minI = 0;
				for (int k = 0; k < agents.size(); k++) {
					Human h = (Human) model.getEntity(agents.get(i));
					double curr = Math.sqrt(Math.pow(clusters[i].centroid[0]
							- h.getX(), 2)
							+ Math.pow(clusters[i].centroid[1] - h.getY(), 2));
					if (curr < min) {
						minI = k;
						min = curr;
					}
				}
				clusters[i].agents.add(agents.remove(minI));
			}
		}
		while (!agents.isEmpty())
			clusters[clusters.length - 1].agents.add(agents.remove(0));

		// write to file
		try {
			FileWriter fw = new FileWriter(fileName);

			for (int i = 0; i < clusters.length; i++) {
				String cluster = "";
				for (EntityID id : clusters[i].cluster)
					cluster += " " + id.toString();
				cluster = cluster.substring(1);

				for (EntityID id : clusters[i].agents) {
					fw.write(id.toString() + "\n");
					fw.write(cluster + "\n");
				}
			}
			fw.close();

		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public Cluster[] distributeClustersNew(StandardWorldModel model,
			ArrayList<ArrayList<EntityID>> mGraphs, double[][] centroids,
			ArrayList<EntityID> agents, String fileName, boolean append,
			boolean writeToFile) {

		// sum of all roads/buildings
		int sum = 0;

		for (int i = 0; i < mGraphs.size(); i++)
			sum += mGraphs.get(i).size();

		// getting perc for all clusters
		Cluster[] clusters = new Cluster[mGraphs.size()];

		for (int i = 0; i < mGraphs.size(); i++) {
			double perc = 1.0 * mGraphs.get(i).size() / sum;
			clusters[i] = new Cluster(mGraphs.get(i), centroids[i], null, perc);
		}

		// sorting clusters
		Arrays.sort(clusters);

		// loop on clusters, assign agents to each
		for (int i = 0; i < clusters.length - 1; i++) {

			// number of agents for cluster
			int num = (int) (clusters[i].perc * agents.size());
			if (num == 0)
				num = 1;

			// assign closest agents
			for (int j = 0; j < num; j++) {
				double min = Double.POSITIVE_INFINITY;
				int minI = 0;
				for (int k = 0; k < agents.size(); k++) {
					Human h = (Human) model.getEntity(agents.get(k));
					double curr = Math.sqrt(Math.pow(clusters[i].centroid[0]
							- h.getX(), 2)
							+ Math.pow(clusters[i].centroid[1] - h.getY(), 2));
					if (curr < min) {
						minI = k;
						min = curr;
					}
				}
				clusters[i].agents.add(agents.remove(minI));
			}
		}
		while (!agents.isEmpty())
			clusters[clusters.length - 1].agents.add(agents.remove(0));

		// write to file
		if (writeToFile)
			try {
				FileWriter fw = new FileWriter(fileName, append);

				for (int i = 0; i < clusters.length; i++) {
					String cluster = "";
					for (EntityID id : clusters[i].cluster)
						cluster += " " + id.toString();
					cluster = cluster.substring(1);

					for (EntityID id : clusters[i].agents) {
						if (append) {
							fw.write(id.toString() + "\n");
							fw.write(cluster + "\n");
						} else {
							fw.append(id.toString() + "\n");
							fw.append(cluster + "\n");
						}

					}
				}
				fw.close();

			} catch (IOException e) {
				e.printStackTrace();
			}
		return clusters;
	}

	public static void assignAgentsToClusters(StandardWorldModel model,
			ArrayList<Cluster> allClusters, ArrayList<EntityID> agents,
			String fileName, boolean append, boolean writeToFile) {

		// loop on clusters, assign agents to each
		for (int i = 0; i < allClusters.size() - 1; i++) {

			// number of agents for cluster
			int num = (int) (allClusters.get(i).perc * agents.size());
			if (num == 0)
				num = 1;

			// assign closest agents
			for (int j = 0; j < num; j++) {
				double min = Double.POSITIVE_INFINITY;
				int minI = 0;
				for (int k = 0; k < agents.size(); k++) {
					Human h = (Human) model.getEntity(agents.get(k));
					double curr = Math.hypot(
							allClusters.get(i).centroid[0] - h.getX(),
							allClusters.get(i).centroid[1] - h.getY());
					if (curr < min) {
						minI = k;
						min = curr;
					}
				}
				allClusters.get(i).agents.add(agents.remove(minI));
			}
		}
		while (!agents.isEmpty())
			allClusters.get(allClusters.size() - 1).agents
					.add(agents.remove(0));

		// write to file
		if (writeToFile)
			try {
				FileWriter fw = new FileWriter(fileName, append);

				for (int i = 0; i < allClusters.size(); i++) {
					String cluster = "";
					for (EntityID id : allClusters.get(i).cluster)
						cluster += " " + id.toString();
					cluster = cluster.substring(1);

					for (EntityID id : allClusters.get(i).agents) {
						if (append) {
							fw.write(id.toString() + "\n");
							fw.write(cluster + "\n");
						} else {
							fw.append(id.toString() + "\n");
							fw.append(cluster + "\n");
						}

					}
				}
				fw.close();

			} catch (IOException e) {
				e.printStackTrace();
			}
	}

	public static void assignAgentsToClusters2(StandardWorldModel model,
			ArrayList<Cluster> allClusters, ArrayList<EntityID> agents,
			String fileName, boolean append, boolean writeToFile) {

		// sorting clusters
		Collections.sort(allClusters);
		ArrayList<EntityID> agentsCopy = Think.copyList(agents);
		// loop on clusters, assign agents to each
		for (int i = 0; i < allClusters.size(); i++) {
			EntityID assignedAgent = null;
			double shortestDistance = Double.POSITIVE_INFINITY;
			for (EntityID agent : agentsCopy) {
				if (model.getDistance(agent, allClusters.get(i).center.getID()) < shortestDistance) {
					shortestDistance = model.getDistance(agent,
							allClusters.get(i).center.getID());
					assignedAgent = agent;
				}

			}
			allClusters.get(i).agents.add(assignedAgent);
			Think.removeIfExists(assignedAgent, agentsCopy);
		}

		// write to file
		if (writeToFile)
			try {
				FileWriter fw = new FileWriter(fileName, append);

				for (int i = 0; i < allClusters.size(); i++) {
					String cluster = "";
					for (EntityID id : allClusters.get(i).cluster)
						cluster += " " + id.toString();
					cluster = cluster.substring(1);
					String ids = "";
					for (EntityID id : allClusters.get(i).agents) {
						ids += id.toString() + " ";
					}
					if (append) {
						fw.write(ids + "\n");
						fw.write(cluster + "\n");
					} else {
						fw.append(ids + "\n");
						fw.append(cluster + "\n");
					}
				}
				fw.close();

			} catch (IOException e) {
				e.printStackTrace();
			}
	}

}
