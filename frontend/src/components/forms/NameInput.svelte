<script>
	import Input from 'flowbite-svelte/Input.svelte';
	import Label from 'flowbite-svelte/Label.svelte';
	import PersonCircleOutline from '~icons/mdi/person-circle-outline';
</script>

<fieldset>
	<Label
		for="user-name"
		class="mb-2 text-xl font-extrabold text-secondary dark:text-dark-secondary"
	>
		User name
	</Label>
	<Input type="text" id="user-name" placeholder="John" required autocomplete="username">
		<PersonCircleOutline slot="left" />
	</Input>
</fieldset>
