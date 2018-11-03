package mil.dds.anet.search;

import org.jdbi.v3.core.Handle;

import mil.dds.anet.beans.Task;
import mil.dds.anet.beans.lists.AnetBeanList;
import mil.dds.anet.beans.search.ISearchQuery;

public interface ITaskSearcher {

	public AnetBeanList<Task> runSearch(ISearchQuery query, Handle dbHandle);
	
	
}
