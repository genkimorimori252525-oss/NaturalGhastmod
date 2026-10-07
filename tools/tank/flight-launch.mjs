/** Reuse a private trial configuration without duplicating Gradle single-value options. */
export function privateFlightLaunchArgs(templateArgs,host,initScript){
 if(!Array.isArray(templateArgs)||templateArgs.length<2||templateArgs.some(a=>typeof a!=='string')||templateArgs.at(-2)!=='--init-script'||
    !templateArgs.at(-1).replaceAll('\\','/').endsWith('/native.init.gradle'))throw Error('PRIVATE_FLIGHT_TEMPLATE_REQUIRED');
 const inherited=templateArgs.slice(0,-2),args=[];
 for(let i=0;i<inherited.length;i++){
  const arg=inherited[i];
  if(arg==='--project-dir'||arg==='-p'){if(!inherited[++i])throw Error('PRIVATE_FLIGHT_TEMPLATE_REQUIRED');continue;}
  if(arg==='--offline'||arg.startsWith('-Pforge_version='))continue;
  args.push(arg);
 }
 return ['--project-dir',host,'-Pforge_version=1.20.1-47.4.10',...args,'--init-script',initScript];
}
