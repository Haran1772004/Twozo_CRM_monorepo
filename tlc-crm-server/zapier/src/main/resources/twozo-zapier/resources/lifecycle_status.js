const PAGE_SIZE = 50;

const lifecycleStatus = async (z, bundle) => {
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
            name: 'lifecycleStageStatusList',
            search: bundle.meta.searchTerm || '',
            limit: PAGE_SIZE
        }
    });
  
    const list = response.data?.data?.list || [];

  return list.map((stage) => ({
    id: stage.id.toString(),
    name: stage.name
  }));
  };
  
  module.exports = {
    key: 'lifecycleStatus_list',
    noun: 'Lifecycle Status List',
    list: {
        display: {
          label: 'List Lifecycle status',
          description: 'Fetches a list of lifecycle statuses for dropdown.',
          hidden: true
        },
        operation: {
          perform: lifecycleStatus,
          canPaginate: true
        },
      },
  };
  