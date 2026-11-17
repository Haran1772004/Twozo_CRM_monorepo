const PAGE_SIZE = 50;

const orgStatus = async (z, bundle) => {
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
            name: 'organizationStatusList',
            search: bundle.meta.searchTerm || '',
            limit: PAGE_SIZE
        }
    });
  
    const list = response.data?.data?.list || [];

  return list.map((org) => ({
    id: org.id.toString(),
    name: org.name
  }));
  };
  
  module.exports = {
    key: 'organizationStatus_list',
    noun: 'Organization Status List',
    list: {
        display: {
          label: 'List Organization status',
          description: 'Fetches a list of organization status for dropdown.',
          hidden: true
        },
        operation: {
          perform: orgStatus,
          canPaginate: true
        },
      },
  };
  