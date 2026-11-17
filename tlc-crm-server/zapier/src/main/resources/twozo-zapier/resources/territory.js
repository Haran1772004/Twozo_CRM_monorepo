const PAGE_SIZE = 50;

const territoryList = async (z, bundle) => {
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
            name: 'territoryList',
            search: bundle.meta.searchTerm || '',
            limit: PAGE_SIZE
        }
    });
  
    const list = response.data?.data?.list || [];

  return list.map((result) => ({
    id: result.id.toString(),
    name: result.name
  }));
  };
  
  module.exports = {
    key: 'territory_list',
    noun: 'Territory List',
    list: {
        display: {
          label: 'List Territory',
          description: 'Fetches a list of territories for dropdown.',
          hidden: true
        },
        operation: {
          perform: territoryList,
          canPaginate: true
        },
      },
  };
  