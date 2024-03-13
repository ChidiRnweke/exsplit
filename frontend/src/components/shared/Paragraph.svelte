<script lang="ts">
	import P from 'flowbite-svelte/P.svelte';
	export let textType: 'regular' | 'primary' | 'secondary' | 'subtle' | 'accent' = 'regular';
	export let exportedClass: string = '';
	$: color = (() => {
		switch (textType) {
			case 'regular':
				return 'text-foreground dark:text-dark-foreground';
			case 'primary':
				return 'text-primary dark:text-dark-primary';
			case 'secondary':
				return 'text-secondary dark:text-dark-secondary';
			case 'subtle':
				return 'text-subtle dark:text-dark-subtle';
			case 'accent':
				return 'text-accent dark:text-dark-accent';
			default:
				return '';
		}
	})();
	export { exportedClass as class };

	$: combinedClasses = `${color} ${exportedClass}`;
</script>

<P class={combinedClasses}><slot /></P>
