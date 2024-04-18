<script lang="ts">
	import Button from 'flowbite-svelte/Button.svelte';
	import Plus from '~icons/mdi/plus';
	import Input from 'flowbite-svelte/Input.svelte';
	import Label from 'flowbite-svelte/Label.svelte';
	import Modal from 'flowbite-svelte/Modal.svelte';
	import Radio from 'flowbite-svelte/Radio.svelte';
	let formModal = false;
</script>

<Button
	class="bg-primary text-xl text-dark-foreground hover:bg-secondary dark:bg-dark-primary hover:dark:bg-dark-secondary"
	on:click={() => (formModal = true)}
>
	<Plus /> Add expense
</Button>

<Modal bind:open={formModal} autoclose={false} class="w-full p-8">
	<form class="flex flex-col space-y-6" action="#">
		<h3 class="mb-4 text-xl font-medium text-foreground dark:text-dark-foreground">Add expense</h3>
		<fieldset class="flex flex-row justify-center gap-x-8">
			<legend class="mb-4 text-sm text-gray-900 dark:text-gray-300">Paid by</legend>
			<Radio name="paid_by">Chidi</Radio>
			<Radio name="paid_by">Ana</Radio>
		</fieldset>
		<Label class="space-y-2">
			<span>Description</span>
			<Input type="text" name="description" placeholder="Dinner at Izakaya" required />
		</Label>
		<Label class="space-y-2">
			<span>Price</span>
			<Input type="number" name="price" placeholder="45" required />
		</Label>
		<Label class="space-y-2">
			<span>Date</span>
			<Input type="date" name="date" placeholder="dd-mm-yyyy" required />
		</Label>
		<Button type="submit" class="w-full bg-primary dark:bg-dark-primary">Add</Button>
	</form>
</Modal>
