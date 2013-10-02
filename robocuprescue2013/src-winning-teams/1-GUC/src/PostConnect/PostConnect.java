package PostConnect;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import clustering.Cluster;
import clustering.Clustering;

import Think.Search;

import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Human;
import rescuecore2.standard.entities.Road;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.standard.entities.StandardWorldModel;
import rescuecore2.worldmodel.EntityID;

public class PostConnect {
	public static ArrayList<EntityID> readAssignedTask(EntityID id,
			StandardWorldModel model, String fileName) {
		ArrayList<EntityID> roadIDs = new ArrayList<EntityID>();
		String path = "";
		FileReader fileReader = null;
		BufferedReader in = null;
		boolean flag = false;
		try {
			fileReader = new FileReader(fileName);
			in = new BufferedReader(fileReader);
			while ((path = in.readLine()) != null)
				if (path.equals(id.toString())) {
					path = in.readLine();
					flag = true;
					break;
				}
			if (in != null)
				in.close();
			if (fileReader != null)
				fileReader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (!flag)
			return roadIDs;
		String[] p = path.split(" ");
		int[] pathIDs = new int[p.length];
		for (int i = 0; i < p.length; i++) {
			pathIDs[i] = Integer.parseInt(p[i]);
		}
		for (int i = 0; i < pathIDs.length; i++)
			for (StandardEntity next : model)
				if (next.getID().getValue() == pathIDs[i])
					roadIDs.add(next.getID());

		return roadIDs;
	}

	public static ArrayList<Cluster> readAllTasks(StandardWorldModel model,
			String fileName) {
		String assignedAgentsString = "";
		String clusterString = "";

		ArrayList<Cluster> clusters = new ArrayList<Cluster>();
		FileReader fileReader = null;
		BufferedReader in = null;
		try {
			fileReader = new FileReader(fileName);
			in = new BufferedReader(fileReader);
			while ((assignedAgentsString = in.readLine()) != null) {
				String[] agents = assignedAgentsString.split(" ");
				int[] agentsIDs = new int[agents.length];
				for (int i = 0; i < agents.length; i++) {
					agentsIDs[i] = Integer.parseInt(agents[i]);
				}

				clusterString = in.readLine();
				String[] p = clusterString.split(" ");
				int[] pathIDs = new int[p.length];
				for (int i = 0; i < p.length; i++) {
					pathIDs[i] = Integer.parseInt(p[i]);
				}
				Cluster currentCluster = new Cluster(null, null, null, 0);

				for (int i = 0; i < agentsIDs.length; i++) {
					EntityID id = new EntityID(agentsIDs[i]);
					currentCluster.agents.add(model.getEntity(id).getID());
				}

				for (int i = 0; i < pathIDs.length; i++) {
					EntityID id = new EntityID(pathIDs[i]);
					currentCluster.cluster.add(model.getEntity(id).getID());
				}
				currentCluster.centroid = Clustering.getCentroid(model,
						currentCluster.cluster);
				currentCluster.center = (Area) Clustering.getClosestEntity(
						model.getEntitiesOfType(StandardEntityURN.BUILDING,
								StandardEntityURN.ROAD),
						currentCluster.centroid[0], currentCluster.centroid[1]);
				clusters.add(currentCluster);

			}
			if (in != null)
				in.close();
			if (fileReader != null)
				fileReader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return clusters;
	}

	public static int getAssignedTaskIndex(EntityID id, String fileName) {
		String path = "";
		FileReader fileReader = null;
		BufferedReader in = null;
		int index = 0;
		try {
			fileReader = new FileReader(fileName);
			in = new BufferedReader(fileReader);
			while ((path = in.readLine()) != null)
				if (path.equals(id.toString()))
					break;
				else
					index++;
			if (in != null)
				in.close();
			if (fileReader != null)
				fileReader.close();
		} catch (IOException e) {
			e.printStackTrace();
			return getNumberOfTasks(fileName) - 1;
		}
		return index / 2;
	}

	public static int getNumberOfTasks(String fileName) {
		FileReader fileReader = null;
		BufferedReader in = null;
		int index = 0;
		try {
			fileReader = new FileReader(fileName);
			in = new BufferedReader(fileReader);
			while (in.readLine() != null)
				index++;
			if (in != null)
				in.close();
			if (fileReader != null)
				fileReader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return index / 2;
	}

	public static void addTask(StandardWorldModel model,
			Collection<StandardEntity> newTask,
			ArrayList<ArrayList<StandardEntity>> tasks, Search search) {
		// tasks.add(new ArrayList<StandardEntity>());
		// ArrayList<StandardEntity> currentTask=tasks.get(tasks.size()-1);
		ArrayList<StandardEntity> currentTask = new ArrayList<StandardEntity>();
		for (StandardEntity next : newTask) {
			if (next instanceof Area) {
				Area r = (Area) next;
				if (r instanceof Road)
					currentTask.add(next);
				else {
					List<EntityID> n = r.getNeighbours();
					for (EntityID ne : n)
						if (model.getEntity(ne) instanceof Road)
							currentTask.add(model.getEntity(ne));
						else {
							ArrayList<StandardEntity> list = (new ArrayList<StandardEntity>(
									model.getEntitiesOfType(StandardEntityURN.POLICE_FORCE)));
							List<EntityID> path = search.breadthFirstSearch(ne,
									((Human) list.get(0)).getPosition());
							for (int i = 0; i < path.size(); i++)
								if (model.getEntity(path.get(i)) instanceof Road) {
									currentTask
											.add(model.getEntity(path.get(i)));
									break;
								}
						}
				}
			} else {
				Human h = (Human) next;
				Area r = (Area) model.getEntity(h.getPosition());
				if (r instanceof Road)
					currentTask.add(r);
				else {
					List<EntityID> n = r.getNeighbours();
					for (EntityID ne : n)
						if (model.getEntity(ne) instanceof Road)
							currentTask.add(model.getEntity(ne));
						else {
							ArrayList<StandardEntity> list = (new ArrayList<StandardEntity>(
									model.getEntitiesOfType(StandardEntityURN.POLICE_FORCE)));
							List<EntityID> path = search.breadthFirstSearch(ne,
									((Human) list.get(0)).getPosition());
							for (int i = 0; i < path.size(); i++)
								if (model.getEntity(path.get(i)) instanceof Road) {
									currentTask
											.add(model.getEntity(path.get(i)));
									break;
								}
						}
				}
			}
		}

		tasks.add(removeDuplicates(currentTask));
		System.out.println("Original number of first task targets: "
				+ newTask.size());
		System.out.println("Final number of first task targets: "
				+ tasks.get(tasks.size() - 1).size());
	}

	public static ArrayList<StandardEntity> formTask(StandardWorldModel model,
			Collection<StandardEntity> newTask, Search search, Human me) {
		// tasks.add(new ArrayList<StandardEntity>());
		// ArrayList<StandardEntity> currentTask=tasks.get(tasks.size()-1);
		ArrayList<StandardEntity> currentTask = new ArrayList<StandardEntity>();

		for (StandardEntity next : newTask) {
			if (next instanceof Area) {
				Area r = (Area) next;
				if (r instanceof Road)
					currentTask.add(next);
				else {
					List<EntityID> n = r.getNeighbours();
					for (EntityID ne : n)
						if (model.getEntity(ne) instanceof Road)
							currentTask.add(model.getEntity(ne));
						else {
							List<EntityID> path = search.breadthFirstSearch(ne,
									(me).getPosition());
							for (int i = 0; i < path.size(); i++)
								if (model.getEntity(path.get(i)) instanceof Road) {
									currentTask
											.add(model.getEntity(path.get(i)));
									break;
								}
						}
				}
			} else {
				Human h = (Human) next;
				Area r = (Area) model.getEntity(h.getPosition());
				if (r instanceof Road)
					currentTask.add(r);
				else {
					List<EntityID> n = r.getNeighbours();
					for (EntityID ne : n)
						if (model.getEntity(ne) instanceof Road)
							currentTask.add(model.getEntity(ne));
						else {

							List<EntityID> path = search.breadthFirstSearch(ne,
									me.getPosition());
							for (int i = 0; i < path.size(); i++)
								if (model.getEntity(path.get(i)) instanceof Road) {
									currentTask
											.add(model.getEntity(path.get(i)));
									break;
								}
						}
				}
			}
		}
		ArrayList<StandardEntity> task = removeDuplicates(currentTask);
		System.out.println("Original number of first task targets: "
				+ newTask.size());
		System.out
				.println("Final number of first task targets: " + task.size());
		return task;
	}

	public static ArrayList<StandardEntity> getFreeAgents(
			StandardWorldModel model,
			Collection<StandardEntity> agentsCollection) {
		ArrayList<StandardEntity> agents = new ArrayList<StandardEntity>();
		for (StandardEntity agent : agentsCollection)
			if (model.getEntity(((Human) agent).getPosition()) instanceof Road)
				agents.add(agent);
		return agents;
	}

	public static ArrayList<StandardEntity> getStuckAgents(
			StandardWorldModel model,
			Collection<StandardEntity> agentsCollection) {
		ArrayList<StandardEntity> agents = new ArrayList<StandardEntity>();
		for (StandardEntity agent : agentsCollection)
			if (model.getEntity(((Human) agent).getPosition()) instanceof Building)
				agents.add(agent);
		return agents;
	}

	public static ArrayList<EntityID> getIDs(ArrayList<StandardEntity> list) {
		ArrayList<EntityID> IDs = new ArrayList<EntityID>();
		for (StandardEntity agent : list)
			IDs.add(agent.getID());
		return IDs;
	}

	public static ArrayList<StandardEntity> removeDuplicates(
			ArrayList<StandardEntity> list) {
		ArrayList<StandardEntity> newList = new ArrayList<StandardEntity>();
		for (StandardEntity en : list)
			if (!exists(en, newList))
				newList.add(en);
		return newList;
	}

	public static boolean exists(StandardEntity target,
			ArrayList<StandardEntity> list) {
		for (StandardEntity en : list)
			if (target.getID().getValue() == en.getID().getValue())
				return true;
		return false;
	}

	public static ArrayList<StandardEntity> getAgentsNotInsideBuildings(
			StandardWorldModel model,
			Collection<StandardEntity> agentsCollection) {
		ArrayList<StandardEntity> agents = new ArrayList<StandardEntity>();
		for (StandardEntity agent : agentsCollection)
			if (model.getEntity(((Human) agent).getPosition()) instanceof Road)
				agents.add(agent);
		return agents;
	}

}
