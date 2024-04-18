<script lang="ts">
	import TableBodyCell from 'flowbite-svelte/TableBodyCell.svelte';
	import TableBodyRow from 'flowbite-svelte/TableBodyRow.svelte';
	import Edit from '~icons/mdi/edit';

	export let user: string;
	export let description: string;
	export let price: string;
	export let date: string;
	export let link: string;
</script>

<TableBodyRow>
	<TableBodyCell class="py-10">{user}</TableBodyCell>
	<TableBodyCell>{description}</TableBodyCell>
	<TableBodyCell>{price}</TableBodyCell>
	<TableBodyCell>{date}</TableBodyCell>
	<TableBodyCell>
		<a href={link} class="text-xl font-extrabold text-accent hover:underline dark:text-dark-accent">
			<Edit />Edit
		</a>
	</TableBodyCell>
</TableBodyRow>
