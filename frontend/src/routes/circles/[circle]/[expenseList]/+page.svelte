<script lang="ts">
	import Table from 'flowbite-svelte/Table.svelte';
	import TableBody from 'flowbite-svelte/TableBody.svelte';
	import TableHead from 'flowbite-svelte/TableHead.svelte';
	import TableHeadCell from '../../../../components/table/TableHeadCell.svelte';
	import HeaderSection from '../../../../components/shared/HeaderSection.svelte';
	import ExpenseRow from '../../../../components/table/ExpenseRow.svelte';
	import PlusButton from '../../../../components/table/PlusButton.svelte';
	import SettleButton from '../../../../components/table/SettleButton.svelte';

	export let data;
</script>

<HeaderSection class="mb-20">Example expense list</HeaderSection>

<div class="text-center">
	<Table
		striped={true}
		class="mb-10 rounded-t-lg border-t-4 border-t-primary dark:border-t-dark-primary"
	>
		<TableHead theadClass="border-b-dark-subtle border-b-2">
			<TableHeadCell>Person</TableHeadCell>
			<TableHeadCell>Description</TableHeadCell>
			<TableHeadCell>Price</TableHeadCell>
			<TableHeadCell>Date</TableHeadCell>
			<TableHeadCell><span class="sr-only">Edit</span></TableHeadCell>
		</TableHead>
		<TableBody>
			{#each data.data as expense (expense.link)}
				<ExpenseRow {...expense} />
			{/each}
		</TableBody>
	</Table>
	<div class="flex flex-row justify-center gap-2">
		<PlusButton />
		<SettleButton />
	</div>
</div>
