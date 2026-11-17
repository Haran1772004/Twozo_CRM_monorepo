const PAGE_SIZE = 50;

const dealList = async (z, bundle) => {
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
            name: 'dealList',
            search: bundle.meta.searchTerm || '',
            limit: PAGE_SIZE
        }
    });
  
    const list = response.data?.data?.list || [];

  return list.map((deal) => ({
    id: deal.id.toString(),
    name: deal.name
  }));
  };
  
  module.exports = {
    key: 'deal_list',
    noun: 'Deal List',
    list: {
        display: {
          label: 'List Deals',
          description: 'Fetches a list of deals for dropdown.',
          hidden: true
        },
        operation: {
          perform: dealList,
          canPaginate: true
        },
      },
  };
  