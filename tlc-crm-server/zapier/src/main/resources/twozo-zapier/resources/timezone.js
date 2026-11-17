const PAGE_SIZE = 50;

const timezoneList = async (z, bundle) => {
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
            name: 'timezoneList',
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
    key: 'timezone_list',
    noun: 'Timezone List',
    list: {
        display: {
          label: 'List Timezones',
          description: 'Fetches a list of timezones for dropdown.',
          hidden: true
        },
        operation: {
          perform: timezoneList,
          canPaginate: true
        },
      },
  };
  