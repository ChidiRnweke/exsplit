<script>
	import H1 from '../../components/shared/H1.svelte';
	import { twMerge } from 'tailwind-merge';

	let headerClass = '';
	export { headerClass as class };
	$: classP = twMerge(headerClass, 'text-center');
</script>

<section class={classP}>
	<H1><slot /></H1>
</section>
