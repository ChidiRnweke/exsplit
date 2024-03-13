<script lang="ts">
	import Plus from '~icons/mdi/plus';
	export let action: (ev: Event) => void = () => {};
</script>

<button on:click|preventDefault={action}>
	<Plus
		class="bg-accent dark:bg-accent hover:bg-primary dark:hover:bg-primary relative h-auto w-10 place-self-center rounded-full text-white lg:place-self-start"
	/>
</button>
