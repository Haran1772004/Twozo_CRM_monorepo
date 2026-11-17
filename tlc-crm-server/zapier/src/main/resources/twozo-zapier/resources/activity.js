const PAGE_SIZE = 50;

const activityList = async (z, bundle) => {
    const response = await z.request({
        method: 'POST',
        url: 'https://crm.twozo.io/api/zap/dropdown',
        headers: {
          'Authorization': `Bearer ${bundle.authData.access_token}`,
          'X-EMAIL': bundle.authData.email,
          'Content-Type': 'application/json'
        },
        body: {
            page: bundle.meta.page,
            name: 'activityList',
            search: bundle.meta.searchTerm || '',
            limit: PAGE_SIZE
        }
    });
  
    const list = response.data?.data?.list || [];

  return list.map((act) => ({
    id: act.id.toString(),
    name: act.name
  }));
  };
  
  module.exports = {
    key: 'activity_list',
    noun: 'Activity List',
    list: {
        display: {
          label: 'List Activities',
          description: 'Fetches a list of activities for dropdown.',
          hidden: true
        },
        operation: {
          perform: activityList,
          canPaginate: true
        },
      },
  };
  