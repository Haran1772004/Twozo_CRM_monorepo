const PAGE_SIZE = 50;

const lifecycleStage = async (z, bundle) => {
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
            name: 'lifeCycleStageList',
            search: bundle.meta.searchTerm || '',
            limit: PAGE_SIZE
        }
    });
  
    const list = response.data?.data?.list || [];

  return list.map((status) => ({
    id: status.id.toString(),
    name: status.name
  }));
  };
  
  module.exports = {
    key: 'lifecycleStage_list',
    noun: 'Lifecycle Stage List',
    list: {
        display: {
          label: 'List Lifecycle stage',
          description: 'Fetches a list of lifecycle stages for dropdown.',
          hidden: true
        },
        operation: {
          perform: lifecycleStage,
          canPaginate: true
        },
      },
  };
  