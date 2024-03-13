<script lang="ts">
	import { twMerge } from 'tailwind-merge';

	export let padding = 'p-16';
	export let formSize: 'sm' | 'md' | 'lg' = 'lg';
	$: size = (() => {
		switch (formSize) {
			case 'sm':
				return '';
			case 'md':
				return 'flex w-full max-w-md';
			case 'lg':
				return 'flex w-full max-w-xl';
			default:
				return '';
		}
	})();
	let defaultClass =
		'grid-cols-1 flex-col place-items-center rounded-md shadow-md border-t-secondary dark:border-t-dark-secondary border-t-4 bg-gray-50 dark:bg-gray-900';

	$: sectionClass = twMerge(defaultClass, padding, size);
</script>

<form class="grid grid-cols-1 place-items-center">
	<section class={sectionClass}>
		<slot />
	</section>
</form>
