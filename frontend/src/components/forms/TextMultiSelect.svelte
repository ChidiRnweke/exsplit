<script lang="ts">
	import MultiSelect from 'flowbite-svelte/MultiSelect.svelte';
	import Input from 'flowbite-svelte/Input.svelte';
	import Check from '~icons/mdi/check.svelte';
	import { createEventDispatcher } from 'svelte';
	import Label from 'flowbite-svelte/Label.svelte';

	let fieldsetClass = '';
	let inputValue = '';

	const dispatch = createEventDispatcher();
	interface MultiSelectItem {
		value: string;
		name: string;
	}
	export let items: Array<MultiSelectItem> = [];
	export let selected: Array<string> = [];

	const handleKeyPress: (event: KeyboardEvent) => void = (event) => {
		if (event.key == 'Enter') {
			event.preventDefault();
			addItem();
		}
	};

	const addItem = () => {
		if (inputValue.trim() && !items.find((item) => item.value === inputValue.trim())) {
			const newItem = { value: inputValue.trim(), name: inputValue.trim() };
			items = [...items, newItem];
			selected = [...selected, inputValue.trim()];
			inputValue = '';
			dispatch('update', selected);
		}
	};

	export { fieldsetClass as class };
	export let textPlaceholder = 'default placeholder';
	export let inputId = 'input';
	export let multiSelectId = 'multiSelect';
	export let labelText = '';
</script>

<fieldset class={fieldsetClass}>
	<Label for={inputId} class="mb-2 text-xl font-extrabold text-secondary dark:text-dark-secondary">
		{labelText}
	</Label>
	<Input
		id={inputId}
		class="mb-4"
		placeholder={textPlaceholder}
		bind:value={inputValue}
		on:keydown={handleKeyPress}
	>
		<button slot="right" on:click|preventDefault={addItem}><Check /></button>
	</Input>

	<MultiSelect id={multiSelectId} {items} bind:value={selected}></MultiSelect>
</fieldset>
