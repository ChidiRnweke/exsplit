<script lang="ts">
	import Table from 'flowbite-svelte/Table.svelte';
	import TableBody from 'flowbite-svelte/TableBody.svelte';
	import TableHead from 'flowbite-svelte/TableHead.svelte';
	import TableHeadCell from '../../../../components/table/TableHeadCell.svelte';
	import HeaderSection from '../../../../components/shared/HeaderSection.svelte';
	import ExpenseRow from '../../../../components/table/ExpenseRow.svelte';
	import PlusButton from '../../../../components/table/PlusButton.svelte';
	import ButtonGroup from 'flowbite-svelte/ButtonGroup.svelte';
	import SettleButton from '../../../../components/table/SettleButton.svelte';

	const exampleData = {
		user: 'John',
		description: 'Magic Mouse 2',
		price: '€1922',
		date: '2021-01-01',
		link: '/tables'
	};
</script>

<HeaderSection class="mb-20">Example expense list</HeaderSection>

<div class="text-center">
	<Table
		striped={true}
		class="border-t-primary dark:border-t-dark-primary mb-10 rounded-t-lg border-t-4"
	>
		<TableHead theadClass="border-b-dark-subtle border-b-2">
			<TableHeadCell>Person</TableHeadCell>
			<TableHeadCell>Description</TableHeadCell>
			<TableHeadCell>Price</TableHeadCell>
			<TableHeadCell>Date</TableHeadCell>
			<TableHeadCell><span class="sr-only">Edit</span></TableHeadCell>
		</TableHead>
		<TableBody>
			<ExpenseRow {...exampleData} />
			<ExpenseRow {...exampleData} />
			<ExpenseRow {...exampleData} />
			<ExpenseRow {...exampleData} />
			<ExpenseRow {...exampleData} />
		</TableBody>
	</Table>
	<div class="flex flex-row justify-center gap-2">
		<PlusButton />
		<SettleButton />
	</div>
</div>
